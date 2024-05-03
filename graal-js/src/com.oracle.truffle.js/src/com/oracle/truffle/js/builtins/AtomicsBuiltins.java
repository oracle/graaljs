/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
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
import com.oracle.truffle.js.runtime.Strings;
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
import com.oracle.truffle.js.runtime.array.TypedArray.TypedBigIntArray;
import com.oracle.truffle.js.runtime.array.TypedArray.TypedIntArray;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint8Array;
import com.oracle.truffle.js.runtime.builtins.BuiltinEnum;
import com.oracle.truffle.js.runtime.builtins.JSArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSArrayBufferView;
import com.oracle.truffle.js.runtime.builtins.JSSharedArrayBuffer;
import com.oracle.truffle.js.runtime.builtins.JSTypedArrayObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Contains builtins for {@linkplain Atomics}.
 */
public final class AtomicsBuiltins extends JSBuiltinsContainer.SwitchEnum<AtomicsBuiltins.Atomics> {

    public static final JSBuiltinsContainer BUILTINS = new AtomicsBuiltins();
    public static final JSBuiltinsContainer WAIT_ASYNC_BUILTINS = new WaitAsyncBuiltins();

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
        wait(4),
        isLockFree(1),

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
        assert context.getEcmaScriptVersion() >= JSConfig.ECMAScript2017;
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
            case notify:
                return AtomicsNotifyNodeGen.create(context, builtin, args().fixedArgs(3).createArgumentNodes(context));
            case wait:
                return AtomicsWaitNodeGen.create(context, builtin, args().fixedArgs(4).createArgumentNodes(context));
            case isLockFree:
                return AtomicsIsLockFreeNodeGen.create(context, builtin, args().fixedArgs(1).createArgumentNodes(context));
        }
        return null;
    }

    public static final class WaitAsyncBuiltins extends JSBuiltinsContainer.SwitchEnum<WaitAsyncBuiltins.WaitAsync> {

        protected WaitAsyncBuiltins() {
            super(WaitAsync.class);
        }

        public enum WaitAsync implements BuiltinEnum<WaitAsync> {
            waitAsync(4);

            private final int length;

            WaitAsync(int length) {
                this.length = length;
            }

            @Override
            public int getLength() {
                return length;
            }
        }

        @Override
        protected Object createNode(JSContext context, JSBuiltin builtin, boolean construct, boolean newTarget, WaitAsync builtinEnum) {
            return switch (builtinEnum) {
                case waitAsync -> AtomicsWaitAsyncNodeGen.create(context, builtin, args().fixedArgs(4).createArgumentNodes(context));
                default -> null;
            };
        }

    }

    @ImportStatic(JSArrayBufferView.class)
    public abstract static class AtomicsOperationNode extends JSBuiltinNode {

        private final BranchProfile detachedBuffer = BranchProfile.create();

        public AtomicsOperationNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        public static boolean isSharedBufferView(JSTypedArrayObject object) {
            return JSArrayBufferView.isJSArrayBufferView(object) && JSSharedArrayBuffer.isJSSharedArrayBuffer(JSArrayBufferView.getArrayBuffer(object));
        }

        public static boolean isInt32SharedBufferView(JSTypedArrayObject object) {
            return isSharedBufferView(object) && JSArrayBufferView.typedArrayGetArrayType(object) instanceof DirectInt32Array;
        }

        public static boolean isDirectInt8Array(TypedArray ta) {
            return ta instanceof DirectInt8Array;
        }

        public static boolean isDirectUint8Array(TypedArray ta) {
            return ta instanceof DirectUint8Array;
        }

        public static boolean isDirectInt16Array(TypedArray ta) {
            return ta instanceof DirectInt16Array;
        }

        public static boolean isDirectUint16Array(TypedArray ta) {
            return ta instanceof DirectUint16Array;
        }

        public static boolean isDirectInt32Array(TypedArray ta) {
            return ta instanceof DirectInt32Array;
        }

        public static boolean isDirectUint32Array(TypedArray ta) {
            return ta instanceof DirectUint32Array;
        }

        public static boolean isDirectBigInt64Array(TypedArray ta) {
            return ta instanceof DirectBigInt64Array;
        }

        public static boolean isDirectBigUint64Array(TypedArray ta) {
            return ta instanceof DirectBigUint64Array;
        }

        protected void checkDetached(JSTypedArrayObject object) {
            if (getContext().getTypedArrayNotDetachedAssumption().isValid()) {
                return;
            }
            if (JSArrayBuffer.isDetachedBuffer(JSArrayBufferView.getArrayBuffer(object))) {
                detachedBuffer.enter();
                throw createTypeErrorNotDetachedArray();
            }
        }

        /* ES8 24.4.1.2 ValidateAtomicAccess */
        protected static int validateAtomicAccess(JSTypedArrayObject target, long convertedIndex, Object originalIndex) {
            int length = JSArrayBufferView.typedArrayGetLength(target);
            assert convertedIndex >= 0;
            if (convertedIndex >= length) {
                throw createRangeErrorSharedArray(originalIndex);
            }
            return (int) convertedIndex;
        }

        protected JSTypedArrayObject validateTypedArray(Object object) {
            if (!JSArrayBufferView.isJSArrayBufferView(object)) {
                throw createTypeErrorNotTypedArray();
            }
            JSTypedArrayObject typedArrayObject = (JSTypedArrayObject) object;
            checkDetached(typedArrayObject);
            return typedArrayObject;
        }

        protected TypedArray validateIntegerTypedArray(JSTypedArrayObject typedArrayObject, boolean waitable) {
            TypedArray ta = JSArrayBufferView.typedArrayGetArrayType(typedArrayObject);
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

        protected Object doCASUint32(JSTypedArrayObject target, int index, int expected, int replacement, TypedArray.TypedIntArray typedArray) {
            return SafeInteger.valueOf(JSRuntime.toUInt32(typedArray.compareExchangeInt(target, index, expected, replacement)));
        }

        protected int doCASInt(JSTypedArrayObject target, int index, int expected, int replacement, TypedArray.TypedIntArray typedArray) {
            return typedArray.compareExchangeInt(target, index, expected, replacement);
        }

        protected BigInt doCASBigInt(JSTypedArrayObject target, int index, BigInt expected, BigInt replacement, TypedArray.TypedBigIntArray typedArray) {
            return typedArray.compareExchangeBigInt(target, index, expected, replacement);
        }

        @TruffleBoundary
        protected static int doInt8(JSTypedArrayObject target, int index, int expected, int replacement, boolean signed, TypedArray.TypedIntArray typedArray) {
            int read = typedArray.getInt(target, index, InteropLibrary.getUncached());
            read = signed ? (byte) read : read & 0xFF;
            int expectedChopped = signed ? (byte) expected : (expected & 0xFF);
            if (read == expectedChopped) {
                int signedValue = signed ? (byte) replacement : (replacement & 0xFF);
                typedArray.setInt(target, index, signedValue, InteropLibrary.getUncached());
            }
            return read;
        }

        @TruffleBoundary
        protected static int doInt16(JSTypedArrayObject target, int index, int expected, int replacement, boolean signed, TypedArray.TypedIntArray typedArray) {
            int read = typedArray.getInt(target, index, InteropLibrary.getUncached());
            read = signed ? (short) read : read & 0xFFFF;
            int expectedChopped = signed ? (short) expected : (expected & 0xFFFF);
            if (read == expectedChopped) {
                int signedValue = signed ? (short) replacement : (replacement & 0xFFFF);
                typedArray.setInt(target, index, signedValue, InteropLibrary.getUncached());
            }
            return read;
        }

        @TruffleBoundary
        protected static int doInt32(JSTypedArrayObject target, int index, int expected, int replacement, TypedArray.TypedIntArray typedArray) {
            int read = typedArray.getInt(target, index, InteropLibrary.getUncached());
            if (read == expected) {
                typedArray.setInt(target, index, replacement, InteropLibrary.getUncached());
            }
            return read;
        }

        @TruffleBoundary
        protected static BigInt doBigInt(JSTypedArrayObject target, int index, BigInt expected, BigInt replacement, TypedArray.TypedBigIntArray typedArray) {
            BigInt read = typedArray.getBigInt(target, index, InteropLibrary.getUncached());
            if (read.compareTo(expected) == 0) {
                typedArray.setBigInt(target, index, replacement, InteropLibrary.getUncached());
            }
            return read;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt8Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doInt8Array(JSTypedArrayObject target, int index, int expected, int replacement,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return (byte) doCASInt(target, index, expected, replacement, (TypedArray.DirectInt8Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint8Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doUint8Array(JSTypedArrayObject target, int index, int expected, int replacement,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return doCASInt(target, index, expected, replacement, (TypedArray.DirectUint8Array) ta) & 0xff;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt16Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doInt16Array(JSTypedArrayObject target, int index, int expected, int replacement,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return (short) doCASInt(target, index, expected, replacement, (TypedArray.DirectInt16Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint16Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doUint16Array(JSTypedArrayObject target, int index, int expected, int replacement,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return doCASInt(target, index, expected, replacement, (TypedArray.DirectUint16Array) ta) & 0xffff;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint32Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected Object doUint32Array(JSTypedArrayObject target, int index, int expected, int replacement,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return doCASUint32(target, index, expected, replacement, (TypedArray.DirectUint32Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt32Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doInt32ArrayInt(JSTypedArrayObject target, int index, int expected, int replacement,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return doCASInt(target, index, expected, replacement, (TypedArray.DirectInt32Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt32Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doInt32ArrayObj(JSTypedArrayObject target, int index, Object expected, Object replacement,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return doCASInt(target, index, toInt(expected), toInt(replacement), (TypedArray.DirectInt32Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt32Array(ta)"})
        protected int doInt32ArrayIntObjIdx(JSTypedArrayObject target, Object index, int expected, int replacement,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASInt(target, intIndex, expected, replacement, (TypedArray.DirectInt32Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt32Array(ta)"})
        protected int doInt32ArrayObjObjIdx(JSTypedArrayObject target, Object index, Object expected, Object replacement,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASInt(target, intIndex, toInt(expected), toInt(replacement), (TypedArray.DirectInt32Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectBigInt64Array(ta)"})
        protected BigInt doBigInt64ArrayObjObjIdx(JSTypedArrayObject target, Object index, Object expected, Object replacement,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASBigInt(target, intIndex, toBigInt(expected).toBigInt64(), toBigInt(replacement), (TypedArray.DirectBigInt64Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectBigUint64Array(ta)"})
        protected BigInt doBigUint64ArrayObjObjIdx(JSTypedArrayObject target, Object index, Object expected, Object replacement,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return doCASBigInt(target, intIndex, toBigInt(expected).toBigUint64(), toBigInt(replacement), (TypedArray.DirectBigUint64Array) ta);
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object expected, Object replacement,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode,
                        @Cached InlinedBranchProfile notSharedArrayBuffer) {

            JSTypedArrayObject target = validateTypedArray(maybeTarget);
            boolean sharedArrayBuffer = isSharedBufferView(target);
            TypedArray ta = validateIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            BigInt expectedBigInt = null;
            BigInt replacementBigInt = null;
            int expectedInt = 0;
            int replacementInt = 0;
            if (ta instanceof TypedBigIntArray) {
                expectedBigInt = toBigInt(expected);
                replacementBigInt = toBigInt(replacement);
            } else {
                expectedInt = toInt(expected);
                replacementInt = toInt(replacement);
            }

            if (!sharedArrayBuffer) {
                notSharedArrayBuffer.enter(this);
                checkDetached(target);
                if (ta instanceof Int8Array || ta instanceof DirectInt8Array || ta instanceof InteropInt8Array) {
                    return (int) (byte) doInt8(target, intIndex, expectedInt, replacementInt, true, (TypedIntArray) ta);
                } else if (ta instanceof Uint8Array || ta instanceof DirectUint8Array || ta instanceof InteropUint8Array) {
                    return doInt8(target, intIndex, expectedInt, replacementInt, false, (TypedIntArray) ta) & 0xff;
                } else if (ta instanceof Int16Array || ta instanceof DirectInt16Array || ta instanceof InteropInt16Array) {
                    return (int) (short) doInt16(target, intIndex, expectedInt, replacementInt, true, (TypedIntArray) ta);
                } else if (ta instanceof Uint16Array || ta instanceof DirectUint16Array || ta instanceof InteropUint16Array) {
                    return doInt16(target, intIndex, expectedInt, replacementInt, false, (TypedIntArray) ta) & 0xffff;
                } else if (ta instanceof Int32Array || ta instanceof DirectInt32Array || ta instanceof InteropInt32Array) {
                    return doInt32(target, intIndex, expectedInt, replacementInt, (TypedIntArray) ta);
                } else if (ta instanceof Uint32Array || ta instanceof DirectUint32Array || ta instanceof InteropUint32Array) {
                    return SafeInteger.valueOf(Integer.toUnsignedLong(doInt32(target, intIndex, expectedInt, replacementInt, (TypedIntArray) ta)));
                } else if (ta instanceof BigInt64Array || ta instanceof DirectBigInt64Array || ta instanceof InteropBigInt64Array) {
                    return doBigInt(target, intIndex, expectedBigInt.toBigInt64(), replacementBigInt, (TypedBigIntArray) ta);
                } else if (ta instanceof BigUint64Array || ta instanceof DirectBigUint64Array || ta instanceof InteropBigUint64Array) {
                    return doBigInt(target, intIndex, expectedBigInt.toBigUint64(), replacementBigInt, (TypedBigIntArray) ta);
                } else {
                    throw Errors.shouldNotReachHereUnexpectedValue(ta);
                }
            } else {
                if (ta instanceof DirectInt8Array direct) {
                    return (int) (byte) doCASInt(target, intIndex, expectedInt, replacementInt, direct);
                } else if (ta instanceof DirectUint8Array direct) {
                    return doCASInt(target, intIndex, expectedInt, replacementInt, direct) & 0xffff;
                } else if (ta instanceof DirectInt16Array direct) {
                    return (int) (short) doCASInt(target, intIndex, expectedInt, replacementInt, direct);
                } else if (ta instanceof DirectUint16Array direct) {
                    return doCASInt(target, intIndex, expectedInt, replacementInt, direct) & 0xffff;
                } else if (ta instanceof DirectInt32Array direct) {
                    return doCASInt(target, intIndex, expectedInt, replacementInt, direct);
                } else if (ta instanceof DirectUint32Array direct) {
                    return doCASUint32(target, intIndex, expectedInt, replacementInt, direct);
                } else if (ta instanceof DirectBigInt64Array direct) {
                    return doCASBigInt(target, intIndex, expectedBigInt.toBigInt64(), replacementBigInt, direct);
                } else if (ta instanceof DirectBigUint64Array direct) {
                    return doCASBigInt(target, intIndex, expectedBigInt.toBigUint64(), replacementBigInt, direct);
                } else {
                    throw Errors.shouldNotReachHereUnexpectedValue(ta);
                }
            }
        }

        private int toInt(Object v) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(JSToInt32Node.create());
            }
            return toIntNode.executeInt(v);
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

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt8Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doInt8ArrayObj(JSTypedArrayObject target, int index,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return SharedMemorySync.doVolatileGet(target, index, (DirectInt8Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint8Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doUint8ArrayObj(JSTypedArrayObject target, int index,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return SharedMemorySync.doVolatileGet(target, index, (DirectUint8Array) ta) & 0xFF;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt16Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doInt16ArrayObj(JSTypedArrayObject target, int index,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return SharedMemorySync.doVolatileGet(target, index, (DirectInt16Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint16Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doUint16ArrayObj(JSTypedArrayObject target, int index,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return SharedMemorySync.doVolatileGet(target, index, (DirectUint16Array) ta) & 0xFFFF;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt32Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doInt32ArrayObj(JSTypedArrayObject target, int index,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return SharedMemorySync.doVolatileGet(target, index, (DirectInt32Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint32Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected SafeInteger doUint32ArrayObj(JSTypedArrayObject target, int index,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return SafeInteger.valueOf(SharedMemorySync.doVolatileGet(target, index, (DirectUint32Array) ta) & 0xFFFFFFFFL);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectBigInt64Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected BigInt doBigInt64ArrayObj(JSTypedArrayObject target, int index,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return SharedMemorySync.doVolatileGetBigInt(target, index, (DirectBigInt64Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectBigUint64Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected BigInt doBigUint64ArrayObj(JSTypedArrayObject target, int index,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return SharedMemorySync.doVolatileGetBigInt(target, index, (DirectBigUint64Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt32Array(ta)"})
        protected int doInt32ArrayObjObjIdx(JSTypedArrayObject target, Object index,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return SharedMemorySync.doVolatileGet(target, intIndex, (DirectInt32Array) ta);
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {

            JSTypedArrayObject target = validateTypedArray(maybeTarget);
            TypedArray ta = validateIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            checkDetached(target);

            if (ta instanceof DirectInt8Array || ta instanceof Int8Array || ta instanceof InteropInt8Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex, (TypedIntArray) ta);
            } else if (ta instanceof DirectUint8Array || ta instanceof Uint8Array || ta instanceof InteropUint8Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex, (TypedIntArray) ta) & 0xFF;
            } else if (ta instanceof DirectInt16Array || ta instanceof Int16Array || ta instanceof InteropInt16Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex, (TypedIntArray) ta);
            } else if (ta instanceof DirectUint16Array || ta instanceof Uint16Array || ta instanceof InteropUint16Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex, (TypedIntArray) ta) & 0xFFFF;
            } else if (ta instanceof DirectInt32Array || ta instanceof Int32Array || ta instanceof InteropInt32Array) {
                return SharedMemorySync.doVolatileGet(target, intIndex, (TypedIntArray) ta);
            } else if (ta instanceof DirectUint32Array || ta instanceof Uint32Array || ta instanceof InteropUint32Array) {
                return SafeInteger.valueOf(SharedMemorySync.doVolatileGet(target, intIndex, (TypedIntArray) ta) & 0xFFFFFFFFL);
            } else if (ta instanceof DirectBigInt64Array || ta instanceof BigInt64Array || ta instanceof InteropBigInt64Array) {
                return SharedMemorySync.doVolatileGetBigInt(target, intIndex, (TypedBigIntArray) ta);
            } else if (ta instanceof DirectBigUint64Array || ta instanceof BigUint64Array || ta instanceof InteropBigUint64Array) {
                return SharedMemorySync.doVolatileGetBigInt(target, intIndex, (TypedBigIntArray) ta);
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

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt8Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doSharedInt8Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            SharedMemorySync.doVolatilePut(target, index, value, (DirectInt8Array) ta);
            return value;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint8Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doSharedUint8Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            SharedMemorySync.doVolatilePut(target, index, value, (DirectUint8Array) ta);
            return value;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt8Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected Number doSharedInt8Array(JSTypedArrayObject target, int index, Object value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            Number v = toIntegerOrInfinity(value);
            SharedMemorySync.doVolatilePut(target, index, toRaw(v), (DirectInt8Array) ta);
            return v;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint8Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected Number doSharedUint8Array(JSTypedArrayObject target, int index, Object value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            Number v = toIntegerOrInfinity(value);
            SharedMemorySync.doVolatilePut(target, index, toRaw(v), (DirectUint8Array) ta);
            return v;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt16Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected Object doSharedInt16Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            SharedMemorySync.doVolatilePut(target, index, (short) value, (DirectInt16Array) ta);
            return value;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint16Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected Object doSharedUint16Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            SharedMemorySync.doVolatilePut(target, index, (short) value, (DirectUint16Array) ta);
            return value;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt16Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected Number doSharedInt16Array(JSTypedArrayObject target, int index, Object value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            Number v = toIntegerOrInfinity(value);
            SharedMemorySync.doVolatilePut(target, index, toRaw(v), (DirectInt16Array) ta);
            return v;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint16Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected Number doSharedUint16Array(JSTypedArrayObject target, int index, Object value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            Number v = toIntegerOrInfinity(value);
            SharedMemorySync.doVolatilePut(target, index, toRaw(v), (DirectUint16Array) ta);
            return v;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt32Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doSharedInt32Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            SharedMemorySync.doVolatilePut(target, index, value, (DirectInt32Array) ta);
            return value;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint32Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doSharedUint32Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            SharedMemorySync.doVolatilePut(target, index, value, (DirectUint32Array) ta);
            return value;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt32Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected Object doSharedInt32Array(JSTypedArrayObject target, int index, Object value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            Number v = toIntegerOrInfinity(value);
            SharedMemorySync.doVolatilePut(target, index, toRaw(v), (DirectInt32Array) ta);
            return v;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint32Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected Object doSharedUint32Array(JSTypedArrayObject target, int index, Object value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            Number v = toIntegerOrInfinity(value);
            SharedMemorySync.doVolatilePut(target, index, toRaw(v), (DirectUint32Array) ta);
            return v;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt32Array(ta)"})
        protected Object doSharedInt32ArrayObjIdx(JSTypedArrayObject target, Object index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            SharedMemorySync.doVolatilePut(target, intIndex, value, (DirectInt32Array) ta);
            return value;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectBigInt64Array(ta)"})
        protected Object doSharedBigInt64Array(JSTypedArrayObject target, Object index, Object value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            BigInt biValue = toBigInt(value, target);
            SharedMemorySync.doVolatilePutBigInt(target, intIndex, biValue, (DirectBigInt64Array) ta);
            return biValue;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectBigUint64Array(ta)"})
        protected Object doSharedBigUint64Array(JSTypedArrayObject target, Object index, Object value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            BigInt biValue = toBigInt(value, target);
            SharedMemorySync.doVolatilePutBigInt(target, intIndex, biValue, (DirectBigUint64Array) ta);
            return biValue;
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object value,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {

            JSTypedArrayObject target = validateTypedArray(maybeTarget);
            TypedArray ta = validateIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            if (ta instanceof DirectInt8Array || ta instanceof DirectUint8Array ||
                            ta instanceof Int8Array || ta instanceof Uint8Array ||
                            ta instanceof InteropInt8Array || ta instanceof InteropUint8Array) {
                Number v = toIntegerOrInfinityChecked(value, target);
                SharedMemorySync.doVolatilePut(target, intIndex, toRaw(v), (TypedIntArray) ta);
                return v;
            } else if (ta instanceof DirectInt16Array || ta instanceof DirectUint16Array ||
                            ta instanceof Int16Array || ta instanceof Uint16Array ||
                            ta instanceof InteropInt16Array || ta instanceof InteropUint16Array) {
                Number v = toIntegerOrInfinityChecked(value, target);
                SharedMemorySync.doVolatilePut(target, intIndex, (short) toRaw(v), (TypedIntArray) ta);
                return v;
            } else if (ta instanceof DirectInt32Array || ta instanceof DirectUint32Array ||
                            ta instanceof Int32Array || ta instanceof Uint32Array ||
                            ta instanceof InteropInt32Array || ta instanceof InteropUint32Array) {
                Number v = toIntegerOrInfinityChecked(value, target);
                SharedMemorySync.doVolatilePut(target, intIndex, toRaw(v), (TypedIntArray) ta);
                return v;
            } else if (ta instanceof DirectBigInt64Array || ta instanceof DirectBigUint64Array ||
                            ta instanceof BigInt64Array || ta instanceof BigUint64Array ||
                            ta instanceof InteropBigInt64Array || ta instanceof InteropBigUint64Array) {
                BigInt v = toBigInt(value, target);
                SharedMemorySync.doVolatilePutBigInt(target, intIndex, v, (TypedBigIntArray) ta);
                return v;
            } else {
                throw Errors.shouldNotReachHere();
            }
        }

        private Number toIntegerOrInfinity(Object value) {
            if (toIntOrInfNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntOrInfNode = insert(JSToIntegerOrInfinityNode.create());
            }
            return toIntOrInfNode.executeNumber(value);
        }

        private Number toIntegerOrInfinityChecked(Object value, JSTypedArrayObject target) {
            Number result = toIntegerOrInfinity(value);
            checkDetached(target);
            return result;
        }

        private int toRaw(Object v) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(JSToInt32Node.create());
            }
            return toIntNode.executeInt(v);
        }

        private BigInt toBigInt(Object v, JSTypedArrayObject target) {
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

        private int atomicDoInt(JSTypedArrayObject target, int index, int value, TypedIntArray typedArray) {
            int initial;
            int result;
            do {
                initial = SharedMemorySync.doVolatileGet(target, index, typedArray);
                result = intOperator.applyAsInt(initial, value);
            } while (!SharedMemorySync.compareAndSetInt(target, index, initial, result, typedArray));
            return initial;
        }

        private BigInt atomicDoBigInt(JSTypedArrayObject target, int index, BigInt value, TypedBigIntArray typedArray) {
            BigInt initial;
            BigInt result;
            do {
                initial = SharedMemorySync.doVolatileGetBigInt(target, index, typedArray);
                result = bigIntOperator.apply(initial, value);
            } while (!SharedMemorySync.compareAndSetBigInt(target, index, initial, result, typedArray));
            return initial;
        }

        @TruffleBoundary
        private int nonAtomicDoInt(JSTypedArrayObject target, int index, int value, TypedIntArray typedArray) {
            int initial = typedArray.getInt(target, index, InteropLibrary.getUncached());
            int result = intOperator.applyAsInt(initial, value);
            typedArray.setInt(target, index, result, InteropLibrary.getUncached());
            return initial;
        }

        @TruffleBoundary
        private BigInt nonAtomicDoBigInt(JSTypedArrayObject target, int index, BigInt value, TypedBigIntArray typedArray) {
            BigInt initial = typedArray.getBigInt(target, index, InteropLibrary.getUncached());
            BigInt result = bigIntOperator.apply(initial, value);
            typedArray.setBigInt(target, index, result, InteropLibrary.getUncached());
            return initial;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt8Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doSharedInt8Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return (byte) atomicDoInt(target, index, value, (DirectInt8Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint8Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doSharedUint8Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return atomicDoInt(target, index, value, (DirectUint8Array) ta) & 0xFF;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt16Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doSharedInt16Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return (short) atomicDoInt(target, index, value, (DirectInt16Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint16Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doSharedUint16Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return atomicDoInt(target, index, value, (DirectUint16Array) ta) & 0xFFFF;
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt32Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected int doSharedInt32Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return atomicDoInt(target, index, value, (DirectInt32Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectUint32Array(ta)", "ta.isInBoundsFast(target, index)"})
        protected SafeInteger doSharedUint32Array(JSTypedArrayObject target, int index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta) {
            return SafeInteger.valueOf(atomicDoInt(target, index, value, (DirectUint32Array) ta) & 0xFFFFFFFFL);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectInt32Array(ta)"})
        protected int doSharedInt32ArrayObjIdx(JSTypedArrayObject target, Object index, int value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return atomicDoInt(target, intIndex, value, (DirectInt32Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectBigInt64Array(ta)"})
        protected BigInt doSharedBigInt64Array(JSTypedArrayObject target, Object index, Object value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return atomicDoBigInt(target, intIndex, toBigInt(value), (DirectBigInt64Array) ta);
        }

        @Specialization(guards = {"isSharedBufferView(target)", "isDirectBigUint64Array(ta)"})
        protected BigInt doSharedBigUint64Array(JSTypedArrayObject target, Object index, Object value,
                        @Bind("typedArrayGetArrayType(target)") TypedArray ta,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode) {
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);
            return atomicDoBigInt(target, intIndex, toBigInt(value), (DirectBigUint64Array) ta);
        }

        @Specialization
        protected Object doGeneric(Object maybeTarget, Object index, Object value,
                        @Cached @Shared("toIndex") JSToIndexNode toIndexNode,
                        @Cached InlinedBranchProfile notSharedArrayBuffer) {

            JSTypedArrayObject target = validateTypedArray(maybeTarget);
            TypedArray ta = validateIntegerTypedArray(target, false);
            int intIndex = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            if (!isSharedBufferView(target)) {
                notSharedArrayBuffer.enter(this);
                if (ta instanceof DirectInt8Array || ta instanceof Int8Array || ta instanceof InteropInt8Array) {
                    return (int) (byte) nonAtomicDoInt(target, intIndex, toIntChecked(value, target), (TypedIntArray) ta);
                } else if (ta instanceof DirectUint8Array || ta instanceof Uint8Array || ta instanceof InteropUint8Array) {
                    return nonAtomicDoInt(target, intIndex, toIntChecked(value, target), (TypedIntArray) ta) & 0xFF;
                } else if (ta instanceof DirectInt16Array || ta instanceof Int16Array || ta instanceof InteropInt16Array) {
                    return nonAtomicDoInt(target, intIndex, toIntChecked(value, target), (TypedIntArray) ta);
                } else if (ta instanceof DirectUint16Array || ta instanceof Uint16Array || ta instanceof InteropUint16Array) {
                    return nonAtomicDoInt(target, intIndex, toIntChecked(value, target), (TypedIntArray) ta) & 0xFFFF;
                } else if (ta instanceof DirectInt32Array || ta instanceof Int32Array || ta instanceof InteropInt32Array) {
                    return nonAtomicDoInt(target, intIndex, toIntChecked(value, target), (TypedIntArray) ta);
                } else if (ta instanceof DirectUint32Array || ta instanceof Uint32Array || ta instanceof InteropUint32Array) {
                    return SafeInteger.valueOf(nonAtomicDoInt(target, intIndex, toIntChecked(value, target), (TypedIntArray) ta) & 0xFFFFFFFFL);
                } else if (ta instanceof DirectBigInt64Array || ta instanceof DirectBigUint64Array ||
                                ta instanceof BigInt64Array || ta instanceof BigUint64Array ||
                                ta instanceof InteropBigInt64Array || ta instanceof InteropBigUint64Array) {
                    return nonAtomicDoBigInt(target, intIndex, toBigIntChecked(value, target), (TypedBigIntArray) ta);
                } else {
                    throw Errors.shouldNotReachHere();
                }
            } else {
                if (ta instanceof DirectInt8Array || ta instanceof Int8Array) {
                    return (int) (byte) atomicDoInt(target, intIndex, toInt(value), (TypedIntArray) ta);
                } else if (ta instanceof DirectUint8Array || ta instanceof Uint8Array) {
                    return atomicDoInt(target, intIndex, toInt(value), (TypedIntArray) ta) & 0xFF;
                } else if (ta instanceof DirectInt16Array || ta instanceof Int16Array) {
                    return (int) (short) atomicDoInt(target, intIndex, toInt(value), (TypedIntArray) ta);
                } else if (ta instanceof DirectUint16Array || ta instanceof Uint16Array) {
                    return atomicDoInt(target, intIndex, toInt(value), (TypedIntArray) ta) & 0xFFFF;
                } else if (ta instanceof DirectInt32Array || ta instanceof Int32Array) {
                    return atomicDoInt(target, intIndex, toInt(value), (TypedIntArray) ta);
                } else if (ta instanceof DirectUint32Array || ta instanceof Uint32Array) {
                    return SafeInteger.valueOf(atomicDoInt(target, intIndex, toInt(value), (TypedIntArray) ta) & 0xFFFFFFFFL);
                } else if (ta instanceof DirectBigInt64Array || ta instanceof DirectBigUint64Array ||
                                ta instanceof BigInt64Array || ta instanceof BigUint64Array) {
                    return atomicDoBigInt(target, intIndex, toBigInt(value), (TypedBigIntArray) ta);
                } else {
                    throw Errors.shouldNotReachHere();
                }
            }
        }

        private int toIntChecked(Object v, JSTypedArrayObject target) {
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

        private BigInt toBigIntChecked(Object v, JSTypedArrayObject target) {
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

    public abstract static class AtomicsNotifyNode extends AtomicsOperationNode {

        public AtomicsNotifyNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected Object doNotify(Object maybeTarget, Object index, Object count,
                        @Cached JSToIndexNode toIndexNode,
                        @Cached JSToInt32Node toInt32Node,
                        @Cached InlinedBranchProfile notSharedArrayBuffer) {

            JSTypedArrayObject target = validateTypedArray(maybeTarget);
            validateIntegerTypedArray(target, true);
            int i = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            int c = Integer.MAX_VALUE;
            if (count != Undefined.instance) {
                int tmp = toInt32Node.executeInt(count);
                c = Integer.max(tmp, 0);
            }
            // Note: this check must happen after 'c' is computed.
            if (!isSharedBufferView(target)) {
                notSharedArrayBuffer.enter(this);
                return 0;
            }

            JSAgentWaiterListEntry wl = SharedMemorySync.getWaiterList(getContext(), target, i);

            return notifyWaiters(wl, c);
        }

        @TruffleBoundary
        private static Object notifyWaiters(JSAgentWaiterListEntry wl, int c) {
            wl.enterCriticalSection();
            try {
                boolean wake = false;
                WaiterRecord[] waiters = SharedMemorySync.removeWaiters(wl, c);
                int n;
                for (n = 0; n < waiters.length; n++) {
                    WaiterRecord waiterRecord = waiters[n];
                    waiterRecord.setNotified();
                    if (waiterRecord.getPromiseCapability() == null) {
                        wake = true;
                    } else {
                        if (Double.isInfinite(waiterRecord.getTimeout())) {
                            waiterRecord.enqueueInAgent();
                        }
                    }
                }
                if (wake) {
                    SharedMemorySync.wakeWaiters(wl);
                }
                return n;
            } finally {
                wl.leaveCriticalSection();
            }
        }
    }

    public abstract static class AtomicsWaitBaseNode extends AtomicsOperationNode {
        private final ConditionProfile isAsyncProfile = ConditionProfile.create();
        private final ConditionProfile timeoutNaNProfile = ConditionProfile.create();
        private final BranchProfile valuesNotEqualBranch = BranchProfile.create();
        private final BranchProfile asyncImmediateTimeoutBranch = BranchProfile.create();
        private final ConditionProfile awokenProfile = ConditionProfile.create();
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
            createAsyncPropertyNode = CreateDataPropertyNode.create(context, Strings.ASYNC);
            createValuePropertyNode = CreateDataPropertyNode.create(context, Strings.VALUE);
        }

        protected AtomicsLoadNode createHelperNode() {
            return AtomicsLoadNodeGen.create(getContext(), getBuiltin(), args().fixedArgs(4).createArgumentNodes(getContext()));
        }

        protected Object doWait(VirtualFrame frame, Object maybeTarget, Object index, Object value, Object timeout, boolean isAsync) {
            JSTypedArrayObject target = validateTypedArray(maybeTarget);
            validateIntegerTypedArray(target, true);
            if (!isSharedBufferView(target)) {
                notSharedArrayBuffer.enter();
                throw createTypeErrorNotSharedArray();
            }
            int i = validateAtomicAccess(target, toIndexNode.executeLong(index), index);

            boolean isInt32 = isInt32SharedBufferView(target);
            long v = isInt32 ? toInt32(value) : toBigInt(value).longValue();
            double t;
            double q = toDoubleNode.executeDouble(timeout);
            if (timeoutNaNProfile.profile(JSRuntime.isNaN(q))) {
                t = Double.POSITIVE_INFINITY;
            } else {
                t = Math.max(q, 0);
            }

            JSAgent agent = getRealm().getAgent();
            if (!isAsync && !agent.canBlock()) {
                errorBranch.enter();
                throw createTypeErrorUnsupported();
            }
            JSAgentWaiterListEntry wl = SharedMemorySync.getWaiterList(getContext(), target, i);

            PromiseCapabilityRecord promiseCapability = null;
            JSDynamicObject resultObject = null;

            if (isAsyncProfile.profile(isAsync)) {
                getContext().signalAsyncWaiterRecordUsage();
                promiseCapability = newPromiseCapability();
                resultObject = ordinaryObjectCreate(frame);
            }

            wl.enterCriticalSection();
            try {
                Object w = loadNode.executeWithBufferAndIndex(frame, maybeTarget, i);
                boolean isNotEqual = isInt32 ? !(w instanceof Integer) || (int) w != (int) v
                                : !(w instanceof BigInt) || ((BigInt) w).longValue() != v;
                if (isNotEqual) {
                    valuesNotEqualBranch.enter();
                    if (!isAsyncProfile.profile(isAsync)) {
                        return Strings.NOT_EQUAL;
                    }
                    createAsyncPropertyNode.executeVoid(resultObject, false);
                    createValuePropertyNode.executeVoid(resultObject, Strings.NOT_EQUAL);
                    return resultObject;
                }

                if (isAsync && t == 0) {
                    asyncImmediateTimeoutBranch.enter();
                    createAsyncPropertyNode.executeVoid(resultObject, false);
                    createValuePropertyNode.executeVoid(resultObject, Strings.TIMED_OUT);
                    return resultObject;
                }
                int id = agent.getSignifier();
                WaiterRecord waiterRecord = WaiterRecord.create(id, promiseCapability, t, Strings.OK, wl, agent);
                SharedMemorySync.addWaiter(agent, wl, waiterRecord, isAsync);

                if (!isAsyncProfile.profile(isAsync)) {
                    boolean awoken = SharedMemorySync.suspendAgent(agent, wl, waiterRecord);
                    if (awokenProfile.profile(awoken)) {
                        assert !wl.contains(waiterRecord);
                        return Strings.OK;
                    } else {
                        SharedMemorySync.removeWaiter(wl, waiterRecord);
                        return Strings.TIMED_OUT;
                    }
                }
                createAsyncPropertyNode.executeVoid(resultObject, true);
                createValuePropertyNode.executeVoid(resultObject, waiterRecord.getPromiseCapability().getPromise());
                return resultObject;
            } finally {
                wl.leaveCriticalSection();
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

        private JSDynamicObject ordinaryObjectCreate(VirtualFrame frame) {
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
        private static final boolean AR_IsLockFree8 = true;

        public AtomicsIsLockFreeNode(JSContext context, JSBuiltin builtin) {
            super(context, builtin);
        }

        @Specialization
        protected static boolean doInt(int size) {
            if (size == 1) {
                return AR_IsLockFree1;
            } else if (size == 2) {
                return AR_IsLockFree2;
            } else if (size == 4) {
                return true;
            } else if (size == 8) {
                return AR_IsLockFree8;
            }
            return false;
        }

        @Specialization
        protected static boolean doGeneric(Object size,
                        @Cached JSToInt32Node toInt32Node) {
            return doInt(toInt32Node.executeInt(size));
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
