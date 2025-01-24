/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.builtins;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidBufferOffsetException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.Boundaries;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSAgentWaiterList;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.array.ByteArrayAccess;
import com.oracle.truffle.js.runtime.array.ByteBufferAccess;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSNonProxyObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.DirectByteBufferHelper;

public abstract sealed class JSArrayBufferObject extends JSNonProxyObject {
    private final int maxByteLength;
    private int byteLength;

    protected JSArrayBufferObject(Shape shape, JSDynamicObject proto, int byteLength, int maxByteLength) {
        super(shape, proto);
        this.byteLength = byteLength;
        this.maxByteLength = maxByteLength;
    }

    @Override
    public TruffleString getClassName() {
        return JSArrayBuffer.CLASS_NAME;
    }

    public abstract void detachArrayBuffer();

    public abstract boolean isDetached();

    public int getByteLength() {
        return byteLength;
    }

    public void setByteLength(int newByteLength) {
        this.byteLength = newByteLength;
    }

    public final int getMaxByteLength() {
        return maxByteLength;
    }

    public final boolean isFixedLength() {
        return (maxByteLength == JSArrayBuffer.FIXED_LENGTH);
    }

    @SuppressWarnings("static-method")
    public final Object getDetachKey() {
        return Undefined.instance;
    }

    public static byte[] getByteArray(Object thisObj) {
        assert JSArrayBuffer.isJSHeapArrayBuffer(thisObj);
        return ((Heap) thisObj).getByteArray();
    }

    public static ByteBuffer getDirectByteBuffer(Object thisObj) {
        assert JSArrayBuffer.isJSDirectArrayBuffer(thisObj) || JSSharedArrayBuffer.isJSSharedArrayBuffer(thisObj);
        return DirectByteBufferHelper.cast(((DirectBase) thisObj).getByteBuffer());
    }

    public static Object getInteropBuffer(Object thisObj) {
        assert JSArrayBuffer.isJSInteropArrayBuffer(thisObj);
        return ((Interop) thisObj).getInteropBuffer();
    }

    public static JSAgentWaiterList getWaiterList(JSDynamicObject thisObj) {
        return ((Shared) thisObj).getWaiterList();
    }

    public static void setWaiterList(JSDynamicObject thisObj, JSAgentWaiterList waiterList) {
        ((Shared) thisObj).setWaiterList(waiterList);
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class Heap extends JSArrayBufferObject {
        byte[] byteArray;

        protected Heap(Shape shape, JSDynamicObject proto, byte[] byteArray, int byteLength, int maxByteLength) {
            super(shape, proto, byteLength, maxByteLength);
            this.byteArray = byteArray;
        }

        public byte[] getByteArray() {
            return byteArray;
        }

        @Override
        public void detachArrayBuffer() {
            this.byteArray = null;
        }

        @Override
        public boolean isDetached() {
            return byteArray == null;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean hasBufferElements() {
            return true;
        }

        @ExportMessage
        long getBufferSize() {
            return isDetached() ? 0 : getByteLength();
        }

        private void ensureNotDetached() throws IndexOutOfBoundsException {
            if (isDetached()) {
                throw BufferIndexOutOfBoundsException.INSTANCE;
            }
        }

        @ExportMessage
        void readBuffer(long byteOffset, byte[] destination, int destinationOffset, int length) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                System.arraycopy(byteArray, checkFromIndexSize(Math.toIntExact(byteOffset), length, byteArray.length),
                                destination, checkFromIndexSize(destinationOffset, length, destination.length), length);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, length);
            }
        }

        private static int checkFromIndexSize(int fromIndex, int size, int length) {
            if ((length | fromIndex | size) < 0 || size > length - fromIndex) {
                throw BufferIndexOutOfBoundsException.INSTANCE;
            }
            return fromIndex;
        }

        @ExportMessage
        byte readBufferByte(long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return byteArray[Math.toIntExact(byteOffset)];
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Byte.BYTES);
            }
        }

        @ExportMessage
        short readBufferShort(ByteOrder order, long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return (short) ByteArrayAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).getInt16(byteArray, Math.toIntExact(byteOffset));
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Short.BYTES);
            }
        }

        @ExportMessage
        int readBufferInt(ByteOrder order, long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return ByteArrayAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).getInt32(byteArray, Math.toIntExact(byteOffset));
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Integer.BYTES);
            }
        }

        @ExportMessage
        long readBufferLong(ByteOrder order, long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return ByteArrayAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).getInt64(byteArray, Math.toIntExact(byteOffset));
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Long.BYTES);
            }
        }

        @ExportMessage
        float readBufferFloat(ByteOrder order, long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return ByteArrayAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).getFloat(byteArray, Math.toIntExact(byteOffset));
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Float.BYTES);
            }
        }

        @ExportMessage
        double readBufferDouble(ByteOrder order, long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return ByteArrayAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).getDouble(byteArray, Math.toIntExact(byteOffset));
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Double.BYTES);
            }
        }

        @ExportMessage
        boolean isBufferWritable() {
            return hasBufferElements();
        }

        @ExportMessage
        void writeBufferByte(long byteOffset, byte value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                byteArray[Math.toIntExact(byteOffset)] = value;
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Byte.BYTES);
            }
        }

        @ExportMessage
        void writeBufferShort(ByteOrder order, long byteOffset, short value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                ByteArrayAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).putInt16(byteArray, Math.toIntExact(byteOffset), value);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Short.BYTES);
            }
        }

        @ExportMessage
        void writeBufferInt(ByteOrder order, long byteOffset, int value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                ByteArrayAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).putInt32(byteArray, Math.toIntExact(byteOffset), value);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Integer.BYTES);
            }
        }

        @ExportMessage
        void writeBufferLong(ByteOrder order, long byteOffset, long value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                ByteArrayAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).putInt64(byteArray, Math.toIntExact(byteOffset), value);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Long.BYTES);
            }
        }

        @ExportMessage
        void writeBufferFloat(ByteOrder order, long byteOffset, float value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                ByteArrayAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).putFloat(byteArray, Math.toIntExact(byteOffset), value);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Float.BYTES);
            }
        }

        @ExportMessage
        void writeBufferDouble(ByteOrder order, long byteOffset, double value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                ByteArrayAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).putDouble(byteArray, Math.toIntExact(byteOffset), value);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Double.BYTES);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public abstract static sealed class DirectBase extends JSArrayBufferObject {
        ByteBuffer byteBuffer;

        protected DirectBase(Shape shape, JSDynamicObject proto, ByteBuffer byteBuffer, int byteLength, int maxByteLength) {
            super(shape, proto, byteLength, maxByteLength);
            this.byteBuffer = byteBuffer;
        }

        public final ByteBuffer getByteBuffer() {
            return byteBuffer;
        }

        public final void setByteBuffer(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
        }

        @Override
        public abstract void detachArrayBuffer();

        @SuppressWarnings("static-method")
        @ExportMessage
        final boolean hasBufferElements() {
            return true;
        }

        @ExportMessage
        final long getBufferSize() {
            return isDetached() ? 0 : getByteLength();
        }

        private void ensureNotDetached() {
            if (isDetached()) {
                throw BufferIndexOutOfBoundsException.INSTANCE;
            }
        }

        @ExportMessage
        final void readBuffer(long byteOffset, byte[] destination, int destinationOffset, int length) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                Boundaries.byteBufferGet(byteBuffer, Math.toIntExact(byteOffset), destination, destinationOffset, length);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, length);
            }
        }

        @ExportMessage
        final byte readBufferByte(long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return byteBuffer.get(Math.toIntExact(byteOffset));
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Byte.BYTES);
            }
        }

        @ExportMessage
        final short readBufferShort(ByteOrder order, long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return (short) ByteBufferAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).getInt16(byteBuffer, Math.toIntExact(byteOffset));
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Short.BYTES);
            }
        }

        @ExportMessage
        final int readBufferInt(ByteOrder order, long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return ByteBufferAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).getInt32(byteBuffer, Math.toIntExact(byteOffset));
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Integer.BYTES);
            }
        }

        @ExportMessage
        final long readBufferLong(ByteOrder order, long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return ByteBufferAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).getInt64(byteBuffer, Math.toIntExact(byteOffset));
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Long.BYTES);
            }
        }

        @ExportMessage
        final float readBufferFloat(ByteOrder order, long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return ByteBufferAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).getFloat(byteBuffer, Math.toIntExact(byteOffset));
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Float.BYTES);
            }
        }

        @ExportMessage
        final double readBufferDouble(ByteOrder order, long byteOffset) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                return ByteBufferAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).getDouble(byteBuffer, Math.toIntExact(byteOffset));
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Double.BYTES);
            }
        }

        @ExportMessage
        final boolean isBufferWritable() {
            return hasBufferElements();
        }

        @ExportMessage
        final void writeBufferByte(long byteOffset, byte value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                byteBuffer.put(Math.toIntExact(byteOffset), value);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Byte.BYTES);
            }
        }

        @ExportMessage
        final void writeBufferShort(ByteOrder order, long byteOffset, short value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                ByteBufferAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).putInt16(byteBuffer, Math.toIntExact(byteOffset), value);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Short.BYTES);
            }
        }

        @ExportMessage
        final void writeBufferInt(ByteOrder order, long byteOffset, int value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                ByteBufferAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).putInt32(byteBuffer, Math.toIntExact(byteOffset), value);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Integer.BYTES);
            }
        }

        @ExportMessage
        final void writeBufferLong(ByteOrder order, long byteOffset, long value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                ByteBufferAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).putInt64(byteBuffer, Math.toIntExact(byteOffset), value);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Long.BYTES);
            }
        }

        @ExportMessage
        final void writeBufferFloat(ByteOrder order, long byteOffset, float value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                ByteBufferAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).putFloat(byteBuffer, Math.toIntExact(byteOffset), value);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Float.BYTES);
            }
        }

        @ExportMessage
        final void writeBufferDouble(ByteOrder order, long byteOffset, double value) throws InvalidBufferOffsetException {
            try {
                ensureNotDetached();
                ByteBufferAccess.forOrder(order == ByteOrder.LITTLE_ENDIAN).putDouble(byteBuffer, Math.toIntExact(byteOffset), value);
            } catch (IndexOutOfBoundsException | ArithmeticException e) {
                throw InvalidBufferOffsetException.create(byteOffset, Double.BYTES);
            }
        }
    }

    public static final class Direct extends DirectBase {

        protected Direct(Shape shape, JSDynamicObject proto, ByteBuffer byteBuffer, int byteLength, int maxByteLength) {
            super(shape, proto, byteBuffer, byteLength, maxByteLength);
        }

        @Override
        public void detachArrayBuffer() {
            this.byteBuffer = null;
        }

        @Override
        public boolean isDetached() {
            return byteBuffer == null;
        }
    }

    public static final class Shared extends DirectBase {
        JSAgentWaiterList waiterList;
        AtomicInteger byteLength;

        protected Shared(Shape shape, JSDynamicObject proto, ByteBuffer byteBuffer, JSAgentWaiterList waiterList, AtomicInteger byteLength, int maxByteLength) {
            super(shape, proto, byteBuffer, /* unused */ -1, maxByteLength);
            this.waiterList = waiterList;
            this.byteLength = byteLength;
        }

        @Override
        public int getByteLength() {
            return byteLength.get();
        }

        public AtomicInteger getByteLengthObject() {
            return byteLength;
        }

        public boolean updateByteLength(int expectedByteLength, int newByteLength) {
            return byteLength.compareAndSet(expectedByteLength, newByteLength);
        }

        public JSAgentWaiterList getWaiterList() {
            return waiterList;
        }

        public void setWaiterList(JSAgentWaiterList waiterList) {
            this.waiterList = waiterList;
        }

        @Override
        public void detachArrayBuffer() {
            throw Errors.unsupported("SharedArrayBuffer cannot be detached");
        }

        @Override
        public boolean isDetached() {
            return false;
        }

        @Override
        public TruffleString getClassName() {
            return JSSharedArrayBuffer.CLASS_NAME;
        }
    }

    /**
     * ArrayBuffer backed by Interop Buffer.
     */
    @ImportStatic(JSConfig.class)
    @ExportLibrary(value = InteropLibrary.class)
    public static final class Interop extends JSArrayBufferObject {
        Object interopBuffer;

        protected Interop(Shape shape, JSDynamicObject proto, Object interopBuffer) {
            super(shape, proto, /* unused */ -1, JSArrayBuffer.FIXED_LENGTH);
            assert InteropLibrary.getUncached().hasBufferElements(interopBuffer);
            this.interopBuffer = interopBuffer;
        }

        public int getByteLength(InteropLibrary interop) {
            try {
                return isDetached() ? 0 : Math.toIntExact(interop.getBufferSize(interopBuffer));
            } catch (UnsupportedMessageException | ArithmeticException e) {
                return 0;
            }
        }

        @Override
        public int getByteLength() {
            return isDetached() ? 0 : getByteLength(InteropLibrary.getUncached(interopBuffer));
        }

        public Object getInteropBuffer() {
            return interopBuffer;
        }

        @Override
        public boolean isDetached() {
            return (interopBuffer == null);
        }

        @Override
        public void detachArrayBuffer() {
            interopBuffer = null;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean hasBufferElements() {
            return true;
        }

        @ExportMessage
        long getBufferSize(
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) {
            if (isDetached()) {
                errorBranch.enter(node);
                return 0;
            } else {
                return getByteLength(interop);
            }
        }

        @ExportMessage
        void readBuffer(long byteOffset, byte[] destination, int destinationOffset, int length,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, length);
            }
            interop.readBuffer(interopBuffer, byteOffset, destination, destinationOffset, length);
        }

        @ExportMessage
        byte readBufferByte(long byteOffset,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Byte.BYTES);
            }
            return interop.readBufferByte(interopBuffer, byteOffset);
        }

        @ExportMessage
        short readBufferShort(ByteOrder order, long byteOffset,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Short.BYTES);
            }
            return interop.readBufferShort(interopBuffer, order, byteOffset);
        }

        @ExportMessage
        int readBufferInt(ByteOrder order, long byteOffset,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Integer.BYTES);
            }
            return interop.readBufferInt(interopBuffer, order, byteOffset);
        }

        @ExportMessage
        long readBufferLong(ByteOrder order, long byteOffset,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Long.BYTES);
            }
            return interop.readBufferLong(interopBuffer, order, byteOffset);
        }

        @ExportMessage
        float readBufferFloat(ByteOrder order, long byteOffset,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Float.BYTES);
            }
            return interop.readBufferFloat(interopBuffer, order, byteOffset);
        }

        @ExportMessage
        double readBufferDouble(ByteOrder order, long byteOffset,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Double.BYTES);
            }
            return interop.readBufferDouble(interopBuffer, order, byteOffset);
        }

        @ExportMessage
        boolean isBufferWritable(@CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException {
            return interop.isBufferWritable(interopBuffer);
        }

        @ExportMessage
        void writeBufferByte(long byteOffset, byte value,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Byte.BYTES);
            }
            interop.writeBufferByte(interopBuffer, byteOffset, value);
        }

        @ExportMessage
        void writeBufferShort(ByteOrder order, long byteOffset, short value,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Short.BYTES);
            }
            interop.writeBufferShort(interopBuffer, order, byteOffset, value);
        }

        @ExportMessage
        void writeBufferInt(ByteOrder order, long byteOffset, int value,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Integer.BYTES);
            }
            interop.writeBufferInt(interopBuffer, order, byteOffset, value);
        }

        @ExportMessage
        void writeBufferLong(ByteOrder order, long byteOffset, long value,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Long.BYTES);
            }
            interop.writeBufferLong(interopBuffer, order, byteOffset, value);
        }

        @ExportMessage
        void writeBufferFloat(ByteOrder order, long byteOffset, float value,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Float.BYTES);
            }
            interop.writeBufferFloat(interopBuffer, order, byteOffset, value);
        }

        @ExportMessage
        void writeBufferDouble(ByteOrder order, long byteOffset, double value,
                        @Bind Node node,
                        @Cached @Cached.Shared("errorBranch") InlinedBranchProfile errorBranch,
                        @CachedLibrary(limit = "InteropLibraryLimit") @Cached.Shared("interop") InteropLibrary interop) throws UnsupportedMessageException, InvalidBufferOffsetException {
            if (isDetached()) {
                errorBranch.enter(node);
                throw InvalidBufferOffsetException.create(byteOffset, Double.BYTES);
            }
            interop.writeBufferDouble(interopBuffer, order, byteOffset, value);
        }
    }

    public static JSArrayBufferObject createHeapArrayBuffer(Shape shape, JSDynamicObject proto, byte[] byteArray) {
        return new Heap(shape, proto, byteArray, byteArray.length, JSArrayBuffer.FIXED_LENGTH);
    }

    @SuppressWarnings("serial")
    static final class BufferIndexOutOfBoundsException extends IndexOutOfBoundsException {
        private static final IndexOutOfBoundsException INSTANCE = new BufferIndexOutOfBoundsException();

        private BufferIndexOutOfBoundsException() {
        }

        @SuppressWarnings("sync-override")
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }
}
