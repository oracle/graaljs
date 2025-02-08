/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.polyglot;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteOrder;
import java.util.Arrays;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.array.TypedArray;
import com.oracle.truffle.js.test.JSTest;

@RunWith(Parameterized.class)
public class ForeignBufferTypedArrayTest {

    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.stream(TypedArray.factories()).//
                        map(typedArrayFactory -> new Object[]{typedArrayFactory.getName().toJavaStringUncached(), typedArrayFactory.getBytesPerElement()}).//
                        toList();
    }

    @Parameter(0) public String typedArrayConstructorName;
    @Parameter(1) public int elementSize;

    /**
     * We should correctly handle interop buffer sizes greater than Integer.MAX_VALUE in the
     * TypedArray constructors. We should either create a TypedArray of the correct size or throw a
     * RangeError if the buffer size is outside our implementation limits.
     */
    @Test
    public void typedArrayOfLargeInteropBuffer() {
        // Set ECMAScript version to staging to enable all TypedArray constructors
        try (Context context = JSTest.newContextBuilder().//
                        option(JSContextOptions.ECMASCRIPT_VERSION_NAME, JSContextOptions.ECMASCRIPT_VERSION_STAGING).//
                        build()) {
            Value arrayConstructor = context.eval("js", typedArrayConstructorName);
            long bufferSize = (long) Integer.MAX_VALUE + elementSize;
            try {
                Value array = arrayConstructor.newInstance(new ForeignZeroBuffer(bufferSize));
                long arrayLength = array.getMember("length").asLong();
                assertEquals(bufferSize / elementSize, arrayLength);
            } catch (PolyglotException e) {
                assertTrue(e.isGuestException());
                assertThat(e.getMessage(), startsWith("RangeError"));
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class ForeignZeroBuffer implements TruffleObject {

        private final long length;

        public ForeignZeroBuffer(long length) {
            this.length = length;
        }

        @ExportMessage
        @SuppressWarnings("unused")
        public static boolean hasBufferElements(ForeignZeroBuffer receiver) {
            return true;
        }

        @ExportMessage
        public long getBufferSize() {
            return length;
        }

        @ExportMessage
        @SuppressWarnings("unused")
        public static byte readBufferByte(ForeignZeroBuffer receiver, long byteOffset) {
            return 0;
        }

        @ExportMessage
        @SuppressWarnings("unused")
        public static short readBufferShort(ForeignZeroBuffer receiver, ByteOrder byteOrder, long byteOffset) {
            return 0;
        }

        @ExportMessage
        @SuppressWarnings("unused")
        public static int readBufferInt(ForeignZeroBuffer receiver, ByteOrder byteOrder, long byteOffset) {
            return 0;
        }

        @ExportMessage
        @SuppressWarnings("unused")
        public static long readBufferLong(ForeignZeroBuffer receiver, ByteOrder byteOrder, long byteOffset) {
            return 0;
        }

        @ExportMessage
        @SuppressWarnings("unused")
        public static float readBufferFloat(ForeignZeroBuffer receiver, ByteOrder byteOrder, long byteOffset) {
            return 0;
        }

        @ExportMessage
        @SuppressWarnings("unused")
        public static double readBufferDouble(ForeignZeroBuffer receiver, ByteOrder byteOrder, long byteOffset) {
            return 0;
        }

        @ExportMessage
        @SuppressWarnings("unused")
        static void readBuffer(ForeignZeroBuffer receiver, long byteOffset, byte[] destination, int destinationOffset, int readLength) {
            Arrays.fill(destination, destinationOffset, destinationOffset + readLength, (byte) 0);
        }
    }
}
