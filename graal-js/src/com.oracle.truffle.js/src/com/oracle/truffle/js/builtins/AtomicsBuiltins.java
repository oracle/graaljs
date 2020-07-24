/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;

import com.oracle.truffle.api.CompilerDirectives;
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
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToIntegerAsLongNode;
import com.oracle.truffle.js.nodes.cast.JSToNumberNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.JSAgentWaiterListEntry;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain Atomics}.
 */
public final class AtomicsBuiltins extends JSBuiltinsContainer.SwitchEnum<AtomicsBuiltins.Atomics> {

    public static final JSBuiltinsContainer BUILTINS = new AtomicsBuiltins();

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
        isLockFree(1),

        // ES9?
        notify(3);

        private final int length;

        Atomics(int length) {
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getECMAScriptVersion() {
            if (this.equals(notify)) {
                return JSConfig.ECMAScript2019;
            }
            return JSConfig.ECMAScript2017;
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
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> a + b, (a, b) -> a.add(b), args().fixedArgs(3).createArgumentNodes(context));
            case sub:
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> a - b, (a, b) -> a.subtract(b), args().fixedArgs(3).createArgumentNodes(context));
            case and:
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> a & b, (a, b) -> a.and(b), args().fixedArgs(3).createArgumentNodes(context));
            case or:
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> a | b, (a, b) -> a.or(b), args().fixedArgs(3).createArgumentNodes(context));
            case xor:
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> a ^ b, (a, b) -> a.xor(b), args().fixedArgs(3).createArgumentNodes(context));
            case exchange:
                return AtomicsComputeNodeGen.create(context, builtin, (a, b) -> b, (a, b) -> b, args().fixedArgs(3).createArgumentNodes(context));
            case wake:
            case notify:
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
            return JSObject.isJSDynamicObject(object) && isSharedBufferView((DynamicObject) object);
        }

        public static boolean isSharedBufferView(DynamicObject object) {
            return JSArrayBufferView.isJSArrayBufferView(object) && JSSharedArrayBuffer.isJSSharedArrayBuffer(JSArrayBufferView.getArrayBuffer(object));
        }

        public static boolean isInt8SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof TypedArray.DirectInt8Array;
        }

        public static boolean isUint8SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof TypedArray.DirectUint8Array;
        }

        public static boolean isInt16SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof TypedArray.DirectInt16Array;
        }

        public static boolean isUint16SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof TypedArray.DirectUint16Array;
        }

        public static boolean isInt32SharedBufferView(Object object) {
            return JSObject.isJSDynamicObject(object) && isInt32SharedBufferView((DynamicObject) object);
        }

        public static boolean isInt32SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof TypedArray.DirectInt32Array;
        }

        public static boolean isUint32SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof TypedArray.DirectUint32Array;
        }

        public static boolean isBigInt64SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof TypedArray.DirectBigInt64Array;
        }

        public static boolean isBigUint64SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof TypedArray.DirectBigUint64Array;
        }

        public static boolean isBigInt64SharedBufferView(Object object) {
            return JSObject.isJSDynamicObject(object) && isBigInt64SharedBufferView((DynamicObject) object);
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

        protected TypedArray validateSharedIntegerTypedArray(DynamicObject object, boolean waitable) {

            if (!isSharedBufferView(object)) {
                throw createTypeErrorNotSharedArray();
            }
            TypedArray ta = JSArrayBufferView.typedArrayGetArrayType(object);

            if (waitable) {
                if (!(ta instanceof TypedArray.DirectInt32Array || ta instanceof TypedArray.DirectBigInt64Array)) {
                    throw createTypeErrorNotWaitableSharedIntArray();
                }
            } else {
                if (!(ta instanceof TypedArray.DirectInt8Array || ta instanceof TypedArray.DirectUint8Array || ta instanceof TypedArray.DirectInt16Array ||
                                ta instanceof TypedArray.DirectUint16Array || ta instanceof TypedArray.DirectInt32Array || ta instanceof TypedArray.DirectUint32Array ||
                                ta instanceof TypedArray.DirectBigInt64Array || ta instanceof TypedArray.DirectBigUint64Array)) {

                    throw createTypeErrorNotSharedIntArray();
                }
            }
            return ta;
        }

        protected DynamicObject ensureDynamicObject(Object maybeTarget) {
            if (!(maybeTarget instanceof DynamicObject)) {
                throw createTypeErrorNotSharedArray();
            }
            return (DynamicObject) maybeTarget;
        }

        public static ByteBuffer getBuffer(DynamicObject thisObj) {
            return JSArrayBufferView.typedArrayGetByteBuffer(thisObj).duplicate();
        }

        protected static boolean inboundFast(DynamicObject target, int index) {
            TypedArray array = JSArrayBufferView.typedArrayGetArrayType(target);
            return array.isInBoundsFast(target, index);
        }

        @TruffleBoundary
        protected final JSException createTypeErrorNotSharedArray() {
            return Errors.createTypeError("Cannot execute on non-shared array.", this);
        }

        @TruffleBoundary
        protected final JSException createTypeErrorNotSharedIntArray() {
            return Errors.createTypeError("Can only execute on selected types of shared int typed arrays " +
                            "(\"Int8Array\", \"Uint8Array\", \"Int16Array\", \"Uint16Array\",  \"Int32Array\", \"Uint32Array\"," +
                            " \"BigUint64Array\", or \"BigInt64Array\").", this);
        }

        @TruffleBoundary
        protected final JSException createTypeErrorNotWaitableSharedIntArray() {
            return Errors.createTypeError("Can only execute on shared Int32Array or BigInt64Array typed arrays.", this);
        }

        @TruffleBoundary
        protected static final JSException createRangeErrorSharedArray(Object idx) {
            return Errors.createRangeError("Range error with index : " + idx);
        }

        @TruffleBoundary
        protected final JSException createTypeErrorUnsupported() {
            return Errors.createTypeError("Unsupported operation", this);
        }
    }

    /**
     * 6.3.3 Atomics.compareExchange(typedArray, index, expectedValue, replacementValue).
     */
    public abstract static class AtomicsCompareExchangeNode extends AtomicsOperationNode {

        @Child private JSToBigIntNode toBigIntNode;
        @Child private JSToIntegerAsLongNode toIntNode;

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
            return SafeInteger.valueOf(SharedMemorySync.atomicFetchOrGetUnsigned(getContext(), target, index, expected, replacement));
        }

        protected int doCASInt(DynamicObject target, int index, int expected, int replacement) {
            return SharedMemorySync.atomicFetchOrGetInt(getContext(), target, index, expected, replacement);
        }

        protected BigInt doCASBigInt(DynamicObject target, int index, BigInt expected, BigInt replacement) {
            return SharedMemorySync.atomicFetchOrGetBigInt(getContext(), target, index, expected, replacement);
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
        protected int doInt32ArrayObj(DynamicObject target, int index, Object expected, Object replacement) {
            return doCASInt(target, index, toInt(expected), toInt(replacement));
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
        protected int doInt32ArrayObjObjIdx(DynamicObject target, Object index,
                        Object expected, Object replacement,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASInt(target, intIndex, toInt(expected), toInt(replacement));
        }

        @Specialization(guards = {"isBigInt64SharedBufferView(target)"})
        protected BigInt doBigInt64ArrayObjObjIdx(DynamicObject target, Object index,
                        Object expected, Object replacement,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASBigInt(target, intIndex, toBigInt(expected).toBigInt64(), toBigInt(replacement));
        }

        @Specialization(guards = {"isBigUint64SharedBufferView(target)"})
        protected BigInt doBigUint64ArrayObjObjIdx(DynamicObject target, Object index,
                        Object expected, Object replacement,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASBigInt(target, intIndex, toBigInt(expected).toBigUint64(), toBigInt(replacement));
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object expected, Object replacement,
                        @Cached("create()") JSToIndexNode toIndexNode) {

            DynamicObject target = ensureDynamicObject(maybeTarget);
            TypedArray ta = validateSharedIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            if (ta instanceof TypedArray.DirectInt8Array) {
                return doCASInt8(target, intIndex, toInt(expected), toInt(replacement), true);
            } else if (ta instanceof TypedArray.DirectUint8Array) {
                return doCASInt8(target, intIndex, toInt(expected), toInt(replacement), false);
            } else if (ta instanceof TypedArray.DirectInt16Array) {
                return doCASInt16(target, intIndex, toInt(expected), toInt(replacement), true);
            } else if (ta instanceof TypedArray.DirectUint16Array) {
                return doCASInt16(target, intIndex, toInt(expected), toInt(replacement), false);
            } else if (ta instanceof TypedArray.DirectInt32Array) {
                return doCASInt(target, intIndex, toInt(expected), toInt(replacement));
            } else if (ta instanceof TypedArray.DirectUint32Array) {
                return doCASUint32(target, intIndex, toInt(expected), toInt(replacement));
            } else if (ta instanceof TypedArray.DirectBigInt64Array) {
                return doCASBigInt(target, intIndex, toBigInt(expected).toBigInt64(), toBigInt(replacement));
            } else if (ta instanceof TypedArray.DirectBigUint64Array) {
                return doCASBigInt(target, intIndex, toBigInt(expected).toBigUint64(), toBigInt(replacement));
            } else {
                throw Errors.shouldNotReachHere();
            }
        }

        private int toInt(Object v) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(JSToIntegerAsLongNode.create());
            }
            return (int) toIntNode.executeLong(v);
        }

        private BigInt toBigInt(Object v) {
            if (toBigIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBigIntNode = insert(JSToBigIntNode.create());
            }
            return toBigIntNode.executeBigInteger(v);
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
        protected int doInt32ArrayObj(DynamicObject target, int index) {
            return SharedMemorySync.doVolatileGet(target, index);
        }

        @Specialization(guards = {"isUint32SharedBufferView(target)", "inboundFast(target,index)"})
        protected SafeInteger doUint32ArrayObj(DynamicObject target, int index) {
            return SafeInteger.valueOf(SharedMemorySync.doVolatileGet(target, index) & 0xFFFFFFFFL);
        }

        @Specialization(guards = {"isBigInt64SharedBufferView(target)", "inboundFast(target,index)"})
        protected BigInt doBigInt64ArrayObj(DynamicObject target, int index) {
            return SharedMemorySync.doVolatileGetBigInt(target, index);
        }

        @Specialization(guards = {"isBigUint64SharedBufferView(target)", "inboundFast(target,index)"})
        protected BigInt doBigUint64ArrayObj(DynamicObject target, int index) {
            return SharedMemorySync.doVolatileGetBigInt(target, index);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)"})
        protected int doInt32ArrayObjObjIdx(DynamicObject target, Object index,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return SharedMemorySync.doVolatileGet(target, intIndex);
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index,
                        @Cached("create()") JSToIndexNode toIndexNode) {

            DynamicObject target = ensureDynamicObject(maybeTarget);
            TypedArray ta = validateSharedIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            if (ta instanceof TypedArray.DirectInt8Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex);
            } else if (ta instanceof TypedArray.DirectUint8Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex) & 0xFF;
            } else if (ta instanceof TypedArray.DirectInt16Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex);
            } else if (ta instanceof TypedArray.DirectUint16Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex) & 0xFFFF;
            } else if (ta instanceof TypedArray.DirectInt32Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex);
            } else if (ta instanceof TypedArray.DirectUint32Array) {
                return SafeInteger.valueOf(SharedMemorySync.doVolatileGet(target, intIndex) & 0xFFFFFFFFL);
            } else if (ta instanceof TypedArray.DirectBigInt64Array) {
                return SharedMemorySync.doVolatileGetBigInt(target, intIndex);
            } else if (ta instanceof TypedArray.DirectBigUint64Array) {
                return SharedMemorySync.doVolatileGetBigInt(target, intIndex);
            } else {
                throw Errors.shouldNotReachHere();
            }
        }
    }

    /**
     * 6.3.11 Atomics.store(typedArray, index, value).
     */
    public abstract static class AtomicsStoreNode extends AtomicsOperationNode {

        @Child private JSToBigIntNode toBigIntNode;
        @Child private JSToIntegerAsLongNode toIntNode;

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
        protected int doIntArrayObj(DynamicObject target, int index, double value) {
            int v = (int) toInt(value);
            SharedMemorySync.doVolatilePut(target, index, v);
            return v;
        }

        @Specialization(guards = {"isInt16SharedBufferView(target)||isUint16SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected Object doInt16ArrayObj(DynamicObject target, int index, int value) {
            SharedMemorySync.doVolatilePut(target, index, (short) value);
            return value;
        }

        @Specialization(guards = {"isInt16SharedBufferView(target)||isUint16SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected int doInt16ArrayObj(DynamicObject target, int index, double value) {
            int v = (int) toInt(value);
            SharedMemorySync.doVolatilePut(target, index, v);
            return v;
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)||isUint32SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected int doInt32ArrayObj(DynamicObject target, int index, int value) {
            SharedMemorySync.doVolatilePut(target, index, value);
            return value;
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)||isUint32SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected Object doInt32ArrayObj(DynamicObject target, int index, double value) {
            long v = toInt(value);
            SharedMemorySync.doVolatilePut(target, index, (int) v);
            return SafeInteger.valueOf(v);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)"})
        protected Object doInt32ArrayObjObjIdx(DynamicObject target, Object index, int value,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            SharedMemorySync.doVolatilePut(target, intIndex, value);
            return value;
        }

        @Specialization(guards = {"isBigInt64SharedBufferView(target) || isBigUint64SharedBufferView(target)"})
        protected Object doBigInt64ArrayObjObjIdx(DynamicObject target, Object index, Object value,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            BigInt biValue = toBigInt(value);
            SharedMemorySync.doVolatilePutBigInt(target, intIndex, biValue);
            return biValue;
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object value,
                        @Cached("create()") JSToIndexNode toIndexNode) {

            DynamicObject target = ensureDynamicObject(maybeTarget);
            TypedArray ta = validateSharedIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            if (ta instanceof TypedArray.DirectInt8Array || ta instanceof TypedArray.DirectUint8Array) {
                int v = (int) toInt(value);
                SharedMemorySync.doVolatilePut(target, intIndex, v);
                return v;
            } else if (ta instanceof TypedArray.DirectInt16Array || ta instanceof TypedArray.DirectUint16Array) {
                int v = (int) toInt(value);
                SharedMemorySync.doVolatilePut(target, intIndex, (short) v);
                return v;
            } else if (ta instanceof TypedArray.DirectInt32Array || ta instanceof TypedArray.DirectUint32Array) {
                long v = toInt(value);
                SharedMemorySync.doVolatilePut(target, intIndex, (int) v);
                return SafeInteger.valueOf(v);
            } else if (ta instanceof TypedArray.DirectBigInt64Array || ta instanceof TypedArray.DirectBigUint64Array) {
                BigInt v = toBigInt(value);
                SharedMemorySync.doVolatilePutBigInt(target, intIndex, v);
                return v;
            } else {
                throw Errors.shouldNotReachHere();
            }
        }

        private long toInt(Object v) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(JSToIntegerAsLongNode.create());
            }
            return toIntNode.executeLong(v);
        }

        private BigInt toBigInt(Object v) {
            if (toBigIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBigIntNode = insert(JSToBigIntNode.create());
            }
            return toBigIntNode.executeBigInteger(v);
        }
    }

    /**
     * Other atomic operations.
     */
    public abstract static class AtomicsComputeNode extends AtomicsOperationNode {

        private final IntBinaryOperator intOperator;
        private final BinaryOperator<BigInt> bigIntOperator;

        @Child private JSToBigIntNode toBigIntNode;
        @Child private JSToIntegerAsLongNode toIntNode;

        public AtomicsComputeNode(JSContext context, JSBuiltin builtin, IntBinaryOperator intOperator, BinaryOperator<BigInt> bigIntOperator) {
            super(context, builtin);
            this.intOperator = intOperator;
            this.bigIntOperator = bigIntOperator;
        }

        private int atomicDoInt(DynamicObject target, int index, int value) {
            int initial;
            int result;
            do {
                initial = SharedMemorySync.doVolatileGet(target, index);
                result = intOperator.applyAsInt(initial, value);
            } while (!SharedMemorySync.compareAndSwapInt(getContext(), target, index, initial, result));
            return initial;
        }

        private BigInt atomicDoBigInt(DynamicObject target, int index, BigInt value) {
            BigInt initial;
            BigInt result;
            do {
                initial = SharedMemorySync.doVolatileGetBigInt(target, index);
                result = bigIntOperator.apply(initial, value);
            } while (!SharedMemorySync.compareAndSwapBigInt(getContext(), target, index, initial, result));
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
        protected int doInt16ArrayObj(DynamicObject target, int index, int value) {
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
        protected SafeInteger doUint32ArrayObj(DynamicObject target, int index, int value) {
            return SafeInteger.valueOf(atomicDoInt(target, index, value) & 0xFFFFFFFFL);
        }

        @Specialization(guards = {"isInt32SharedBufferView(target)"})
        protected int doInt32ArrayObjObjIdx(DynamicObject target, Object index, int value,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return atomicDoInt(target, intIndex, value);
        }

        @Specialization(guards = {"isBigInt64SharedBufferView(target) || isBigUint64SharedBufferView(target)"})
        protected BigInt doBigInt64ArrayObjObjIdx(DynamicObject target, Object index, Object value,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return atomicDoBigInt(target, intIndex, toBigInt(value));
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object value,
                        @Cached("create()") JSToIndexNode toIndexNode) {

            DynamicObject target = ensureDynamicObject(maybeTarget);
            TypedArray ta = validateSharedIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            if (ta instanceof TypedArray.DirectInt8Array) {
                return (int) (byte) atomicDoInt(target, intIndex, toInt(value));
            } else if (ta instanceof TypedArray.DirectUint8Array) {
                return ((byte) atomicDoInt(target, intIndex, toInt(value))) & 0xFF;
            } else if (ta instanceof TypedArray.DirectInt16Array) {
                return atomicDoInt(target, intIndex, toInt(value));
            } else if (ta instanceof TypedArray.DirectUint16Array) {
                return (atomicDoInt(target, intIndex, toInt(value))) & 0xFFFF;
            } else if (ta instanceof TypedArray.DirectInt32Array) {
                return atomicDoInt(target, intIndex, toInt(value));
            } else if (ta instanceof TypedArray.DirectUint32Array) {
                return SafeInteger.valueOf(atomicDoInt(target, intIndex, toInt(value)) & 0xFFFFFFFFL);
            } else if (ta instanceof TypedArray.DirectBigInt64Array || ta instanceof TypedArray.DirectBigUint64Array) {
                return atomicDoBigInt(target, intIndex, toBigInt(value));
            } else {
                throw Errors.shouldNotReachHere();
            }
        }

        private int toInt(Object v) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(JSToIntegerAsLongNode.create());
            }
            return (int) toIntNode.executeLong(v);
        }

        private BigInt toBigInt(Object v) {
            if (toBigIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBigIntNode = insert(JSToBigIntNode.create());
            }
            return toBigIntNode.executeBigInteger(v);
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

            DynamicObject target = ensureDynamicObject(maybeTarget);
            validateSharedIntegerTypedArray(target, true);
            int i = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            int c = Integer.MAX_VALUE;
            if (count != Undefined.instance) {
                int tmp = toInt32Node.executeInt(count);
                c = Integer.max(tmp, 0);
            }

            JSAgentWaiterListEntry wl = SharedMemorySync.getWaiterList(getContext(), target, i);

            SharedMemorySync.enterCriticalSection(getContext(), wl);
            try {
                int[] waiters = SharedMemorySync.removeWaiters(getContext(), wl, c);
                int n;
                for (n = 0; n < waiters.length; n++) {
                    SharedMemorySync.wakeWaiter(getContext(), waiters[n]);
                }
                return n;
            } finally {
                SharedMemorySync.leaveCriticalSection(getContext(), wl);
            }
        }
    }

    /**
     * Thread Sleep.
     */
    public abstract static class AtomicsWaitNode extends AtomicsOperationNode {

        private static final String OK = "ok";
        private static final String NOT_EQUAL = "not-equal";
        private static final String TIMED_OUT = "timed-out";

        @Child private JSToBigIntNode toBigIntNode;
        @Child private JSToInt32Node toInt32Node;

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

            DynamicObject target = ensureDynamicObject(maybeTarget);
            validateSharedIntegerTypedArray(target, true);

            int i = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            boolean isInt32 = isInt32SharedBufferView(maybeTarget);
            long v = isInt32 ? toInt32(value) : toBigInt(value).longValue();
            int t = Integer.MAX_VALUE;
            Number tmp = timeToInt32Node.executeNumber(timeout);
            if (!JSRuntime.isNaN(tmp)) {
                t = Integer.max(tmp.intValue(), 0);
            }

            if (!SharedMemorySync.agentCanSuspend(getContext())) {
                throw createTypeErrorUnsupported();
            }
            JSAgentWaiterListEntry wl = SharedMemorySync.getWaiterList(getContext(), target, i);
            SharedMemorySync.enterCriticalSection(getContext(), wl);
            try {
                Object w = loadNode.executeWithBufferAndIndex(frame, maybeTarget, i);
                boolean isNotEqual = isInt32 ? !(w instanceof Integer) || (int) w != (int) v
                                : !(w instanceof BigInt) || ((BigInt) w).longValue() != v;
                if (isNotEqual) {
                    return NOT_EQUAL;
                }
                int id = getContext().getJSAgent().getSignifier();
                SharedMemorySync.addWaiter(getContext(), wl, id);
                if (t < 0) {
                    return TIMED_OUT;
                }
                boolean awoken = SharedMemorySync.suspendAgent(getContext(), wl, id, t);
                if (awoken) {
                    assert !wl.contains(id);
                    return OK;
                } else {
                    SharedMemorySync.removeWaiter(getContext(), wl, id);
                    return TIMED_OUT;
                }
            } finally {
                SharedMemorySync.leaveCriticalSection(getContext(), wl);
            }
        }

        private int toInt32(Object v) {
            if (toInt32Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toInt32Node = insert(JSToInt32Node.create());
            }
            return toInt32Node.executeInt(v);
        }

        private BigInt toBigInt(Object v) {
            if (toBigIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBigIntNode = insert(JSToBigIntNode.create());
            }
            return toBigIntNode.executeBigInteger(v);
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
            } else if (n == 8) {
                // BigInt related
                return !getContext().getContextOptions().isTestV8Mode();
            }
            return false;
        }
    }
}
