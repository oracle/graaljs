/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsCompareExchangeNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsComputeNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsIsLockFreeNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsLoadNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsNotifyNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsStoreNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsWaitAsyncNodeGen;
import com.oracle.truffle.js.builtins.AtomicsBuiltinsFactory.AtomicsWaitNodeGen;
import com.oracle.truffle.js.builtins.helper.SharedMemorySync;
import com.oracle.truffle.js.nodes.access.CreateDataPropertyNode;
import com.oracle.truffle.js.nodes.access.CreateObjectNode;
import com.oracle.truffle.js.nodes.cast.JSToBigIntNode;
import com.oracle.truffle.js.nodes.cast.JSToDoubleNode;
import com.oracle.truffle.js.nodes.cast.JSToIndexNode;
import com.oracle.truffle.js.nodes.cast.JSToInt32Node;
import com.oracle.truffle.js.nodes.cast.JSToIntegerOrInfinityNode;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltinNode;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.runtime.BigInt;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSAgent;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.JSAgentWaiterListEntry;
import com.oracle.truffle.js.runtime.JSAgentWaiterList.WaiterRecord;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.SafeInteger;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.runtime.array.TypedArray.BigInt64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.BigUint64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectBigInt64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectBigUint64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectInt16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectInt32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectInt8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectUint16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectUint32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectUint8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Int16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Int32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Int8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropBigInt64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropBigUint64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropInt16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropInt32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropInt8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropUint16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropUint32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropUint8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint8Array;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

import static com.oracle.truffle.js.runtime.builtins.JSArrayBuffer.isDetachedBuffer;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.getArrayBuffer;
import static com.oracle.truffle.js.runtime.builtins.JSArrayBufferView.typedArrayGetArrayType;

/**
 * Contains builtins for {@linkplain Atomics}.
 */
public final class AtomicsBuiltins extends JSBuiltinsContainer.SwitchEnum<AtomicsBuiltins.Atomics> {

    public static final JSBuiltinsContainer BUILTINS = new AtomicsBuiltins();
    public static final JSBuiltinsContainer WAIT_ASYNC_BUILTIN = new AtomicsWaitAsyncBuiltin();

    public static final String OK = "ok";
    public static final String NOT_EQUAL = "not-equal";
    public static final String TIMED_OUT = "timed-out";

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
                return AtomicsNotifyNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case wait:
                return AtomicsWaitNodeGen.create(context, builtin, args().fixedArgs(4).createArgumentNodes(context));
            case isLockFree:
                return AtomicsIsLockFreeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public static final class AtomicsWaitAsyncBuiltin extends JSBuiltinsContainer.SwitchEnum<AtomicsWaitAsyncBuiltin.AtomicsWaitAsync> {
        protected AtomicsWaitAsyncBuiltin() {
            super(JSRealm.ATOMICS_CLASS_NAME, AtomicsWaitAsync.class);
        }

        public enum AtomicsWaitAsync implements BuiltinEnum<AtomicsWaitAsync> {
            waitAsync(4);

            private final int length;

            AtomicsWaitAsync(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, AtomicsWaitAsync builtinEnum) {
            switch (builtinEnum) {
                case waitAsync:
                    return AtomicsWaitAsyncNodeGen.create(context, builtin, args().fixedArgs(4).createArgumentNodes(context));
            }
            return null;
        }
    }

    public abstract static class AtomicsOperationNode extends JSBuiltinNode {

        private final BranchProfile detachedBuffer = BranchProfile.create();

        public AtomicsOperationNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        public static boolean isSharedBufferView(Object object) {
            return JSDynamicObject.isJSDynamicObject(object) && isSharedBufferView((DynamicObject) object);
        }

        public static boolean isSharedBufferView(DynamicObject object) {
            return JSArrayBufferView.isJSArrayBufferView(object) && JSSharedArrayBuffer.isJSSharedArrayBuffer(JSArrayBufferView.getArrayBuffer(object));
        }

        public static boolean isInt8SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof DirectInt8Array;
        }

        public static boolean isUint8SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof DirectUint8Array;
        }

        public static boolean isInt16SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof DirectInt16Array;
        }

        public static boolean isUint16SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof DirectUint16Array;
        }

        public static boolean isInt32SharedBufferView(Object object) {
            return JSDynamicObject.isJSDynamicObject(object) && isInt32SharedBufferView((DynamicObject) object);
        }

        public static boolean isInt32SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof DirectInt32Array;
        }

        public static boolean isUint32SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof DirectUint32Array;
        }

        public static boolean isBigInt64SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof DirectBigInt64Array;
        }

        public static boolean isBigUint64SharedBufferView(DynamicObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof DirectBigUint64Array;
        }

        public static boolean isBigInt64SharedBufferView(Object object) {
            return JSDynamicObject.isJSDynamicObject(object) && isBigInt64SharedBufferView((DynamicObject) object);
        }

        protected void checkDetached(DynamicObject object) {
            if (getContext().getTypedArrayNotDetachedAssumption().isValid()) {
                return;
            }
            if (isDetachedBuffer(getArrayBuffer(object))) {
                detachedBuffer.enter();
                throw createTypeErrorNotDetachedArray();
            }
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

        protected TypedArray validateTypedArray(DynamicObject object) {
            if (!JSArrayBufferView.isJSArrayBufferView(object)) {
                throw createTypeErrorNotTypedArray();
            }
            checkDetached(object);
            return JSArrayBufferView.typedArrayGetArrayType(object);
        }

        protected TypedArray validateIntegerTypedArray(DynamicObject object, boolean waitable) {
            TypedArray ta = validateTypedArray(object);
            if (waitable) {
                if (!(ta instanceof DirectInt32Array || ta instanceof DirectBigInt64Array || ta instanceof Int32Array || ta instanceof BigInt64Array || ta instanceof InteropInt32Array ||
                                ta instanceof InteropBigInt64Array)) {
                    throw createTypeErrorNotWaitableIntArray();
                }
            } else {
                if (!(ta instanceof DirectInt8Array || ta instanceof DirectUint8Array || ta instanceof DirectInt16Array ||
                                ta instanceof DirectUint16Array || ta instanceof DirectInt32Array || ta instanceof DirectUint32Array ||
                                ta instanceof DirectBigInt64Array || ta instanceof DirectBigUint64Array ||
                                ta instanceof Int8Array || ta instanceof Uint8Array || ta instanceof Int16Array ||
                                ta instanceof Uint16Array || ta instanceof Int32Array || ta instanceof Uint32Array ||
                                ta instanceof BigInt64Array || ta instanceof BigUint64Array ||
                                ta instanceof InteropInt8Array || ta instanceof InteropUint8Array || ta instanceof InteropInt16Array ||
                                ta instanceof InteropUint16Array || ta instanceof InteropInt32Array || ta instanceof InteropUint32Array ||
                                ta instanceof InteropBigInt64Array || ta instanceof InteropBigUint64Array)) {
                    throw createTypeErrorNotIntArray();
                }
            }
            return ta;
        }

        protected DynamicObject ensureDynamicObject(Object maybeTarget) {
            if (!(maybeTarget instanceof DynamicObject)) {
                throw createTypeErrorNotTypedArray();
            }
            return (DynamicObject) maybeTarget;
        }

        protected static boolean inboundFast(DynamicObject target, int index) {
            TypedArray array = JSArrayBufferView.typedArrayGetArrayType(target);
            return array.isInBoundsFast(target, index);
        }

        @TruffleBoundary
        protected final JSException createTypeErrorNotDetachedArray() {
            return Errors.createTypeError("Cannot execute on detached array.", this);
        }

        @TruffleBoundary
        protected final JSException createTypeErrorNotTypedArray() {
            return Errors.createTypeError("Cannot execute on non-typed array.", this);
        }

        @TruffleBoundary
        protected final JSException createTypeErrorNotSharedArray() {
            return Errors.createTypeError("Cannot execute on non-shared array.", this);
        }

        @TruffleBoundary
        protected final JSException createTypeErrorNotIntArray() {
            return Errors.createTypeError("Can only execute on selected types of int typed arrays " +
                            "(\"Int8Array\", \"Uint8Array\", \"Int16Array\", \"Uint16Array\",  \"Int32Array\", \"Uint32Array\"," +
                            " \"BigUint64Array\", or \"BigInt64Array\").", this);
        }

        @TruffleBoundary
        protected final JSException createTypeErrorNotWaitableIntArray() {
            return Errors.createTypeError("Can only execute on Int32Array or BigInt64Array typed arrays.", this);
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
        @Child private JSToInt32Node toIntNode;

        public AtomicsCompareExchangeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        protected int doCASInt8(DynamicObject target, int index, int expected, int replacement, boolean signed) {
            return SharedMemorySync.atomicFetchOrGetByte(getRealm().getAgent(), target, index, (byte) expected, replacement, signed);
        }

        protected int doCASInt16(DynamicObject target, int index, int expected, int replacement, boolean signed) {
            return SharedMemorySync.atomicFetchOrGetShort(getRealm().getAgent(), target, index, expected, replacement, signed);
        }

        protected Object doCASUint32(DynamicObject target, int index, Object expected, Object replacement) {
            return SafeInteger.valueOf(SharedMemorySync.atomicFetchOrGetUnsigned(getRealm().getAgent(), target, index, expected, replacement));
        }

        protected int doCASInt(DynamicObject target, int index, int expected, int replacement) {
            return SharedMemorySync.atomicFetchOrGetInt(getRealm().getAgent(), target, index, expected, replacement);
        }

        protected BigInt doCASBigInt(DynamicObject target, int index, BigInt expected, BigInt replacement) {
            return SharedMemorySync.atomicFetchOrGetBigInt(getRealm().getAgent(), target, index, expected, replacement);
        }

        @TruffleBoundary
        protected int doInt8(DynamicObject target, int index, int expected, int replacement, boolean signed) {
            TypedArray array = typedArrayGetArrayType(target);
            TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) array;
            int read = typedArray.getInt(target, index, InteropLibrary.getUncached());
            read = signed ? read : read & 0xFF;
            int expectedChopped = signed ? (byte) expected : expected & 0xFF;
            if (read == expectedChopped) {
                int signedValue = signed ? replacement : replacement & 0xFF;
                typedArray.setInt(target, index, (byte) signedValue, InteropLibrary.getUncached());
            }
            return read;
        }

        @TruffleBoundary
        protected int doInt16(DynamicObject target, int index, int expected, int replacement, boolean signed) {
            TypedArray array = typedArrayGetArrayType(target);
            TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) array;
            int read = typedArray.getInt(target, index, InteropLibrary.getUncached());
            read = signed ? read : read & 0xFFFF;
            int expectedChopped = signed ? (short) expected : expected & 0xFFFF;
            if (read == expectedChopped) {
                int signedValue = signed ? replacement : replacement & 0xFFFF;
                typedArray.setInt(target, index, (short) signedValue, InteropLibrary.getUncached());
            }
            return read;
        }

        @TruffleBoundary
        protected Object doUint32(DynamicObject target, int index, Object expected, Object replacement) {
            TypedArray array = typedArrayGetArrayType(target);
            TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) array;
            long read = JSRuntime.toUInt32(typedArray.getInt(target, index, InteropLibrary.getUncached()));
            if (read == JSRuntime.toUInt32(expected)) {
                typedArray.setInt(target, index, (int) JSRuntime.toUInt32(replacement), InteropLibrary.getUncached());
            }
            return SafeInteger.valueOf(read);
        }

        @TruffleBoundary
        protected int doInt(DynamicObject target, int index, int expected, int replacement) {
            TypedArray array = typedArrayGetArrayType(target);
            TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) array;
            int read = typedArray.getInt(target, index, InteropLibrary.getUncached());
            if (read == expected) {
                typedArray.setInt(target, index, replacement, InteropLibrary.getUncached());
            }
            return read;
        }

        @TruffleBoundary
        protected BigInt doBigInt(DynamicObject target, int index, BigInt expected, BigInt replacement) {
            TypedArray array = typedArrayGetArrayType(target);
            TypedArray.TypedBigIntArray typedArray = (TypedArray.TypedBigIntArray) array;
            BigInt read = typedArray.getBigInt(target, index, InteropLibrary.getUncached());
            if (read.compareTo(expected) == 0) {
                typedArray.setBigInt(target, index, replacement, InteropLibrary.getUncached());
            }
            return read;
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
            return doCASInt(target, index, toInt(expected), toIntChecked(replacement, target));
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
            return doCASInt(target, intIndex, toInt(expected), toIntChecked(replacement, target));
        }

        @Specialization(guards = {"isBigInt64SharedBufferView(target)"})
        protected BigInt doBigInt64ArrayObjObjIdx(DynamicObject target, Object index,
                        Object expected, Object replacement,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASBigInt(target, intIndex, toBigInt(expected).toBigInt64(), toBigIntChecked(replacement, target));
        }

        @Specialization(guards = {"isBigUint64SharedBufferView(target)"})
        protected BigInt doBigUint64ArrayObjObjIdx(DynamicObject target, Object index,
                        Object expected, Object replacement,
                        @Cached("create()") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASBigInt(target, intIndex, toBigInt(expected).toBigUint64(), toBigIntChecked(replacement, target));
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object expected, Object replacement,
                        @Cached("create()") JSToIndexNode toIndexNode,
                        @Cached("create()") BranchProfile notSharedArrayBuffer) {

            DynamicObject target = ensureDynamicObject(maybeTarget);
            TypedArray ta = validateIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            if (!isSharedBufferView(target)) {
                notSharedArrayBuffer.enter();
                if (ta instanceof Int8Array || ta instanceof DirectInt8Array || ta instanceof InteropInt8Array) {
                    return doInt8(target, intIndex, toInt(expected), toIntChecked(replacement, target), true);
                } else if (ta instanceof Uint8Array || ta instanceof DirectUint8Array || ta instanceof InteropUint8Array) {
                    return doInt8(target, intIndex, toInt(expected), toIntChecked(replacement, target), false);
                } else if (ta instanceof Int16Array || ta instanceof DirectInt16Array || ta instanceof InteropInt16Array) {
                    return doInt16(target, intIndex, toInt(expected), toIntChecked(replacement, target), true);
                } else if (ta instanceof Uint16Array || ta instanceof DirectUint16Array || ta instanceof InteropUint16Array) {
                    return doInt16(target, intIndex, toInt(expected), toIntChecked(replacement, target), false);
                } else if (ta instanceof Int32Array || ta instanceof DirectInt32Array || ta instanceof InteropInt32Array) {
                    return doInt(target, intIndex, toInt(expected), toIntChecked(replacement, target));
                } else if (ta instanceof Uint32Array || ta instanceof DirectUint32Array || ta instanceof InteropUint32Array) {
                    return doUint32(target, intIndex, toInt(expected), toIntChecked(replacement, target));
                } else if (ta instanceof BigInt64Array || ta instanceof DirectBigInt64Array || ta instanceof InteropBigInt64Array) {
                    return doBigInt(target, intIndex, toBigInt(expected).toBigInt64(), toBigIntChecked(replacement, target));
                } else if (ta instanceof BigUint64Array || ta instanceof DirectBigUint64Array || ta instanceof InteropBigUint64Array) {
                    return doBigInt(target, intIndex, toBigInt(expected).toBigUint64(), toBigIntChecked(replacement, target));
                } else {
                    throw Errors.shouldNotReachHere();
                }
            } else {
                if (ta instanceof Int8Array || ta instanceof DirectInt8Array) {
                    return doCASInt8(target, intIndex, toInt(expected), toIntChecked(replacement, target), true);
                } else if (ta instanceof Uint8Array || ta instanceof DirectUint8Array) {
                    return doCASInt8(target, intIndex, toInt(expected), toIntChecked(replacement, target), false);
                } else if (ta instanceof Int16Array || ta instanceof DirectInt16Array) {
                    return doCASInt16(target, intIndex, toInt(expected), toIntChecked(replacement, target), true);
                } else if (ta instanceof Uint16Array || ta instanceof DirectUint16Array) {
                    return doCASInt16(target, intIndex, toInt(expected), toIntChecked(replacement, target), false);
                } else if (ta instanceof Int32Array || ta instanceof DirectInt32Array) {
                    return doCASInt(target, intIndex, toInt(expected), toIntChecked(replacement, target));
                } else if (ta instanceof Uint32Array || ta instanceof DirectUint32Array) {
                    return doCASUint32(target, intIndex, toInt(expected), toIntChecked(replacement, target));
                } else if (ta instanceof BigInt64Array || ta instanceof DirectBigInt64Array) {
                    return doCASBigInt(target, intIndex, toBigInt(expected).toBigInt64(), toBigIntChecked(replacement, target));
                } else if (ta instanceof BigUint64Array || ta instanceof DirectBigUint64Array) {
                    return doCASBigInt(target, intIndex, toBigInt(expected).toBigUint64(), toBigIntChecked(replacement, target));
                } else {
                    throw Errors.shouldNotReachHere();
                }
            }
        }

        private int toIntChecked(Object v, DynamicObject target) {
            int value = toInt(v);
            checkDetached(target);
            return value;
        }

        private int toInt(Object v) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(JSToInt32Node.create());
            }
            return toIntNode.executeInt(v);
        }

        private BigInt toBigIntChecked(Object v, DynamicObject target) {
            BigInt result = toBigInt(v);
            checkDetached(target);
            return result;
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
            TypedArray ta = validateIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            checkDetached(target);

            if (ta instanceof DirectInt8Array || ta instanceof Int8Array || ta instanceof InteropInt8Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex);
            } else if (ta instanceof DirectUint8Array || ta instanceof Uint8Array || ta instanceof InteropUint8Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex) & 0xFF;
            } else if (ta instanceof DirectInt16Array || ta instanceof Int16Array || ta instanceof InteropInt16Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex);
            } else if (ta instanceof DirectUint16Array || ta instanceof Uint16Array || ta instanceof InteropUint16Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex) & 0xFFFF;
            } else if (ta instanceof DirectInt32Array || ta instanceof Int32Array || ta instanceof InteropInt32Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex);
            } else if (ta instanceof DirectUint32Array || ta instanceof Uint32Array || ta instanceof InteropUint32Array) {
                return SafeInteger.valueOf(SharedMemorySync.doVolatileGet(target, intIndex) & 0xFFFFFFFFL);
            } else if (ta instanceof DirectBigInt64Array || ta instanceof BigInt64Array || ta instanceof InteropBigInt64Array) {
                return SharedMemorySync.doVolatileGetBigInt(target, intIndex);
            } else if (ta instanceof DirectBigUint64Array || ta instanceof BigUint64Array || ta instanceof InteropBigUint64Array) {
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

        @Child private JSToInt32Node toIntNode;
        @Child private JSToBigIntNode toBigIntNode;
        @Child private JSToIntegerOrInfinityNode toIntOrInfNode;

        public AtomicsStoreNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization(guards = {"isInt8SharedBufferView(target)||isUint8SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected int doIntArrayObj(DynamicObject target, int index, int value) {
            SharedMemorySync.doVolatilePut(target, index, value);
            return value;
        }

        @Specialization(guards = {"isInt8SharedBufferView(target)||isUint8SharedBufferView(target)",
                        "inboundFast(target,index)"})
        protected Number doIntArrayObj(DynamicObject target, int index, double value) {
            Number v = toIntOrInf(value);
            SharedMemorySync.doVolatilePut(target, index, toRaw(v, target));
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
        protected Number doInt16ArrayObj(DynamicObject target, int index, double value) {
            Number v = toIntOrInf(value);
            SharedMemorySync.doVolatilePut(target, index, toRaw(v, target));
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
            Number v = toIntOrInf(value);
            SharedMemorySync.doVolatilePut(target, index, toRaw(v, target));
            return v;
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
            BigInt biValue = toBigInt(value, target);
            SharedMemorySync.doVolatilePutBigInt(target, intIndex, biValue);
            return biValue;
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object value,
                        @Cached("create()") JSToIndexNode toIndexNode) {

            DynamicObject target = ensureDynamicObject(maybeTarget);
            TypedArray ta = validateIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            checkDetached(target);

            if (ta instanceof DirectInt8Array || ta instanceof DirectUint8Array ||
                            ta instanceof Int8Array || ta instanceof Uint8Array ||
                            ta instanceof InteropInt8Array || ta instanceof InteropUint8Array) {
                Number v = toIntOrInf(value);
                SharedMemorySync.doVolatilePut(target, intIndex, toRaw(v, target));
                return v;
            } else if (ta instanceof DirectInt16Array || ta instanceof DirectUint16Array ||
                            ta instanceof Int16Array || ta instanceof Uint16Array ||
                            ta instanceof InteropInt16Array || ta instanceof InteropUint16Array) {
                Number v = toIntOrInf(value);
                SharedMemorySync.doVolatilePut(target, intIndex, (short) toRaw(v, target));
                return v;
            } else if (ta instanceof DirectInt32Array || ta instanceof DirectUint32Array ||
                            ta instanceof Int32Array || ta instanceof Uint32Array ||
                            ta instanceof InteropInt32Array || ta instanceof InteropUint32Array) {
                Number v = toIntOrInf(value);
                SharedMemorySync.doVolatilePut(target, intIndex, toRaw(v, target));
                return v;
            } else if (ta instanceof DirectBigInt64Array || ta instanceof DirectBigUint64Array ||
                            ta instanceof BigInt64Array || ta instanceof BigUint64Array ||
                            ta instanceof InteropBigInt64Array || ta instanceof InteropBigUint64Array) {
                BigInt v = toBigInt(value, target);
                SharedMemorySync.doVolatilePutBigInt(target, intIndex, v);
                return v;
            } else {
                throw Errors.shouldNotReachHere();
            }
        }

        private Number toIntOrInf(Object value) {
            if (toIntOrInfNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntOrInfNode = insert(JSToIntegerOrInfinityNode.create());
            }
            return toIntOrInfNode.executeNumber(value);
        }

        private int toRaw(Object v, DynamicObject target) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(JSToInt32Node.create());
            }
            int result = toIntNode.executeInt(v);
            checkDetached(target);
            return result;
        }

        private BigInt toBigInt(Object v, DynamicObject target) {
            if (toBigIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBigIntNode = insert(JSToBigIntNode.create());
            }
            BigInt result = toBigIntNode.executeBigInteger(v);
            checkDetached(target);
            return result;
        }
    }

    /**
     * Other atomic operations.
     */
    public abstract static class AtomicsComputeNode extends AtomicsOperationNode {

        private final AtomicIntBinaryOperator intOperator;
        private final AtomicBinaryOperator<BigInt> bigIntOperator;

        @Child private JSToBigIntNode toBigIntNode;
        @Child private JSToInt32Node toIntNode;

        public AtomicsComputeNode(JSContext context, JSBuiltin builtin, AtomicIntBinaryOperator intOperator, AtomicBinaryOperator<BigInt> bigIntOperator) {
            super(context, builtin);
            this.intOperator = intOperator;
            this.bigIntOperator = bigIntOperator;
        }

        private int atomicDoInt(DynamicObject target, int index, int value) {
            JSAgent agent = getRealm().getAgent();
            int initial;
            int result;
            do {
                initial = SharedMemorySync.doVolatileGet(target, index);
                result = intOperator.applyAsInt(initial, value);
            } while (!SharedMemorySync.compareAndSwapInt(agent, target, index, initial, result));
            return initial;
        }

        private BigInt atomicDoBigInt(DynamicObject target, int index, BigInt value) {
            JSAgent agent = getRealm().getAgent();
            BigInt initial;
            BigInt result;
            do {
                initial = SharedMemorySync.doVolatileGetBigInt(target, index);
                result = bigIntOperator.apply(initial, value);
            } while (!SharedMemorySync.compareAndSwapBigInt(agent, target, index, initial, result));
            return initial;
        }

        @TruffleBoundary
        private int nonAtomicDoInt(DynamicObject target, int index, int value) {
            TypedArray array = typedArrayGetArrayType(target);
            TypedArray.TypedIntArray typedArray = (TypedArray.TypedIntArray) array;
            int initial = typedArray.getInt(target, index, InteropLibrary.getUncached());
            int result = intOperator.applyAsInt(initial, value);
            typedArray.setInt(target, index, result, InteropLibrary.getUncached());
            return initial;
        }

        @TruffleBoundary
        private BigInt nonAtomicDoBigInt(DynamicObject target, int index, BigInt value) {
            TypedArray array = typedArrayGetArrayType(target);
            TypedArray.TypedBigIntArray typedArray = (TypedArray.TypedBigIntArray) array;
            BigInt initial = typedArray.getBigInt(target, index, InteropLibrary.getUncached());
            BigInt result = bigIntOperator.apply(initial, value);
            typedArray.setBigInt(target, index, result, InteropLibrary.getUncached());
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
            return atomicDoBigInt(target, intIndex, toBigInt(value, target));
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object value,
                        @Cached("create()") JSToIndexNode toIndexNode,
                        @Cached("create()") BranchProfile notSharedArrayBuffer) {

            DynamicObject target = ensureDynamicObject(maybeTarget);
            TypedArray ta = validateIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            if (!isSharedBufferView(target)) {
                notSharedArrayBuffer.enter();
                if (ta instanceof DirectInt8Array || ta instanceof Int8Array || ta instanceof InteropInt8Array) {
                    return (int) (byte) nonAtomicDoInt(target, intIndex, toInt(value, target));
                } else if (ta instanceof DirectUint8Array || ta instanceof Uint8Array || ta instanceof InteropUint8Array) {
                    return ((byte) nonAtomicDoInt(target, intIndex, toInt(value, target))) & 0xFF;
                } else if (ta instanceof DirectInt16Array || ta instanceof Int16Array || ta instanceof InteropInt16Array) {
                    return nonAtomicDoInt(target, intIndex, toInt(value, target));
                } else if (ta instanceof DirectUint16Array || ta instanceof Uint16Array || ta instanceof InteropUint16Array) {
                    return (nonAtomicDoInt(target, intIndex, toInt(value, target))) & 0xFFFF;
                } else if (ta instanceof DirectInt32Array || ta instanceof Int32Array || ta instanceof InteropInt32Array) {
                    return nonAtomicDoInt(target, intIndex, toInt(value, target));
                } else if (ta instanceof DirectUint32Array || ta instanceof Uint32Array || ta instanceof InteropUint32Array) {
                    return SafeInteger.valueOf(nonAtomicDoInt(target, intIndex, toInt(value, target)) & 0xFFFFFFFFL);
                } else if (ta instanceof DirectBigInt64Array || ta instanceof DirectBigUint64Array ||
                                ta instanceof BigInt64Array || ta instanceof BigUint64Array ||
                                ta instanceof InteropBigInt64Array || ta instanceof InteropBigUint64Array) {
                    return nonAtomicDoBigInt(target, intIndex, toBigInt(value, target));
                } else {
                    throw Errors.shouldNotReachHere();
                }
            } else {
                if (ta instanceof DirectInt8Array || ta instanceof Int8Array) {
                    return (int) (byte) atomicDoInt(target, intIndex, toInt(value, target));
                } else if (ta instanceof DirectUint8Array || ta instanceof Uint8Array) {
                    return ((byte) atomicDoInt(target, intIndex, toInt(value, target))) & 0xFF;
                } else if (ta instanceof DirectInt16Array || ta instanceof Int16Array) {
                    return atomicDoInt(target, intIndex, toInt(value, target));
                } else if (ta instanceof DirectUint16Array || ta instanceof Uint16Array) {
                    return (atomicDoInt(target, intIndex, toInt(value, target))) & 0xFFFF;
                } else if (ta instanceof DirectInt32Array || ta instanceof Int32Array) {
                    return atomicDoInt(target, intIndex, toInt(value, target));
                } else if (ta instanceof DirectUint32Array || ta instanceof Uint32Array) {
                    return SafeInteger.valueOf(atomicDoInt(target, intIndex, toInt(value, target)) & 0xFFFFFFFFL);
                } else if (ta instanceof DirectBigInt64Array || ta instanceof DirectBigUint64Array ||
                                ta instanceof BigInt64Array || ta instanceof BigUint64Array) {
                    return atomicDoBigInt(target, intIndex, toBigInt(value, target));
                } else {
                    throw Errors.shouldNotReachHere();
                }
            }
        }

        private int toInt(Object v, DynamicObject target) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(JSToInt32Node.create());
            }
            long result = toIntNode.executeInt(v);
            checkDetached(target);
            return (int) result;
        }

        private BigInt toBigInt(Object v, DynamicObject target) {
            if (toBigIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toBigIntNode = insert(JSToBigIntNode.create());
            }
            BigInt result = toBigIntNode.executeBigInteger(v);
            checkDetached(target);
            return result;
        }
    }

    public abstract static class AtomicsNotifyNode extends AtomicsOperationNode {

        public AtomicsNotifyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object count,
                        @Cached("create()") JSToIndexNode toIndexNode,
                        @Cached("create()") JSToInt32Node toInt32Node,
                        @Cached("create()") BranchProfile notSharedArrayBuffer) {

            DynamicObject target = ensureDynamicObject(maybeTarget);
            validateIntegerTypedArray(target, true);
            int i = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            int c = Integer.MAX_VALUE;
            if (count != Undefined.instance) {
                int tmp = toInt32Node.executeInt(count);
                c = Integer.max(tmp, 0);
            }
            // Note: this check must happen after 'c' is computed.
            if (!isSharedBufferView(target)) {
                notSharedArrayBuffer.enter();
                return 0;
            }

            JSAgent agent = getRealm().getAgent();
            JSAgentWaiterListEntry wl = SharedMemorySync.getWaiterList(getContext(), agent, target, i);

            SharedMemorySync.enterCriticalSection(agent, wl);
            try {
                WaiterRecord[] waiters = SharedMemorySync.removeWaiters(agent, wl, c);
                int n;
                for (n = 0; n < waiters.length; n++) {
                    if (waiters[n].getPromiseCapability() == null) {
                        SharedMemorySync.notifyWaiter(agent, waiters[n]);
                    } else {
                        waiters[n].setNotified();
                        if (Double.isInfinite(waiters[n].getTimeout())) {
                            waiters[n].enqueueInAgent();
                        }
                    }
                }
                return n;
            } finally {
                SharedMemorySync.leaveCriticalSection(agent, wl);
            }
        }
    }

    public abstract static class AtomicsWaitBaseNode extends AtomicsOperationNode {
        private final ConditionProfile isAsyncProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile timeoutNaNProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile valuesNotEqualBranch = BranchProfile.create();
        private final BranchProfile asyncImmediateTimeoutBranch = BranchProfile.create();
        private final ConditionProfile awokenProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile errorBranch = BranchProfile.create();
        private final BranchProfile notSharedArrayBuffer = BranchProfile.create();

        @Child private JSToIndexNode toIndexNode;
        @Child private JSToDoubleNode toDoubleNode;
        @Child private AtomicsLoadNode loadNode;
        @Child private JSToBigIntNode toBigIntNode;
        @Child private JSToInt32Node toInt32Node;
        @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
        @Child private CreateObjectNode objectCreateNode;
        @Child private CreateDataPropertyNode createAsyncPropertyNode;
        @Child private CreateDataPropertyNode createValuePropertyNode;

        public AtomicsWaitBaseNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
            toIndexNode = JSToIndexNode.create();
            toDoubleNode = JSToDoubleNode.create();
            loadNode = createHelperNode();
            createAsyncPropertyNode = CreateDataPropertyNode.create(context, "async");
            createValuePropertyNode = CreateDataPropertyNode.create(context, "value");
        }

        protected AtomicsLoadNode createHelperNode() {
            return AtomicsLoadNodeGen.create(getContext(), getBuiltin(), args().fixedArgs(4).createArgumentNodes(getContext()));
        }

        protected Object doWait(VirtualFrame frame, Object maybeTarget, Object index, Object value, Object timeout, boolean isAsync) {
            DynamicObject target = ensureDynamicObject(maybeTarget);
            validateIntegerTypedArray(target, true);
            if (!isSharedBufferView(target)) {
                notSharedArrayBuffer.enter();
                throw createTypeErrorNotSharedArray();
            }
            int i = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            boolean isInt32 = isInt32SharedBufferView(maybeTarget);
            long v = isInt32 ? toInt32(value) : toBigInt(value).longValue();
            double t;
            double q = toDoubleNode.executeDouble(timeout);
            if (timeoutNaNProfile.profile(JSRuntime.isNaN(q))) {
                t = Double.POSITIVE_INFINITY;
            } else {
                t = Math.max(q, 0);
            }

            JSAgent agent = getRealm().getAgent();
            if (!isAsync && !SharedMemorySync.agentCanSuspend(agent)) {
                errorBranch.enter();
                throw createTypeErrorUnsupported();
            }
            JSAgentWaiterListEntry wl = SharedMemorySync.getWaiterList(getContext(), agent, target, i);

            PromiseCapabilityRecord promiseCapability = null;
            DynamicObject resultObject = null;

            if (isAsyncProfile.profile(isAsync)) {
                getContext().signalAsyncWaiterRecordUsage();
                promiseCapability = newPromiseCapability();
                resultObject = ordinaryObjectCreate(frame);
            }

            SharedMemorySync.enterCriticalSection(agent, wl);
            try {
                Object w = loadNode.executeWithBufferAndIndex(frame, maybeTarget, i);
                boolean isNotEqual = isInt32 ? !(w instanceof Integer) || (int) w != (int) v
                                : !(w instanceof BigInt) || ((BigInt) w).longValue() != v;
                if (isNotEqual) {
                    valuesNotEqualBranch.enter();
                    if (!isAsyncProfile.profile(isAsync)) {
                        return NOT_EQUAL;
                    }
                    createAsyncPropertyNode.executeVoid(resultObject, false);
                    createValuePropertyNode.executeVoid(resultObject, NOT_EQUAL);
                    return resultObject;
                }

                if (isAsync && t == 0) {
                    asyncImmediateTimeoutBranch.enter();
                    createAsyncPropertyNode.executeVoid(resultObject, false);
                    createValuePropertyNode.executeVoid(resultObject, TIMED_OUT);
                    return resultObject;
                }
                int id = agent.getSignifier();
                WaiterRecord waiterRecord = WaiterRecord.create(id, promiseCapability, t, OK, wl, agent);
                SharedMemorySync.addWaiter(agent, wl, waiterRecord, isAsync);

                if (!isAsyncProfile.profile(isAsync)) {
                    boolean awoken = SharedMemorySync.suspendAgent(agent, wl, waiterRecord);
                    if (awokenProfile.profile(awoken)) {
                        assert !wl.contains(waiterRecord);
                        return OK;
                    } else {
                        SharedMemorySync.removeWaiter(agent, wl, waiterRecord);
                        return TIMED_OUT;
                    }
                }
                createAsyncPropertyNode.executeVoid(resultObject, true);
                createValuePropertyNode.executeVoid(resultObject, waiterRecord.getPromiseCapability().getPromise());
                return resultObject;
            } finally {
                SharedMemorySync.leaveCriticalSection(agent, wl);
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

        private PromiseCapabilityRecord newPromiseCapability() {
            if (newPromiseCapabilityNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                newPromiseCapabilityNode = insert(NewPromiseCapabilityNode.create(getContext()));
            }
            return newPromiseCapabilityNode.executeDefault();
        }

        private DynamicObject ordinaryObjectCreate(VirtualFrame frame) {
            if (objectCreateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                objectCreateNode = insert(CreateObjectNode.create(getContext()));
            }
            return objectCreateNode.execute(frame);
        }
    }

    public abstract static class AtomicsWaitNode extends AtomicsWaitBaseNode {

        public AtomicsWaitNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doGeneric(VirtualFrame frame, Object maybeTarget, Object index, Object value, Object timeout) {
            return doWait(frame, maybeTarget, index, value, timeout, false);
        }
    }

    public abstract static class AtomicsWaitAsyncNode extends AtomicsWaitBaseNode {

        public AtomicsWaitAsyncNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doGeneric(VirtualFrame frame, Object maybeTarget, Object index, Object value, Object timeout) {
            return doWait(frame, maybeTarget, index, value, timeout, true);
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

    @FunctionalInterface
    public interface AtomicIntBinaryOperator {
        int applyAsInt(int left, int right);
    }

    @FunctionalInterface
    public interface AtomicBinaryOperator<T> {
        T apply(T t, T u);
    }
}
