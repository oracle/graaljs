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
package com.oracle.truffle.js.codec;

import java.nio.ByteBuffer;

public interface NodeDecoder<F> {
    class DecoderState {
        private final BinaryDecoder decoder;
        private final Object[] objRegs;
        private final Object[] arguments;

        public DecoderState(BinaryDecoder decoder, Object[] arguments) {
            this.decoder = decoder;
            this.objRegs = new Object[getUInt()];
            this.arguments = arguments;
        }

        public DecoderState(BinaryDecoder decoder) {
            this(decoder, new Object[0]);
        }

        public Object getObjReg(int index) {
            return objRegs[index];
        }

        public void setObjReg(int index, Object value) {
            objRegs[index] = value;
        }

        public Object getObject() {
            return getObjReg(getReg());
        }

        public int getInt() {
            return decoder.getInt();
        }

        public int getUInt() {
            return decoder.getUInt();
        }

        public long getLong() {
            return decoder.getLong();
        }

        public boolean getBoolean() {
            return decoder.getInt() != 0;
        }

        public double getDouble() {
            return decoder.getDouble();
        }

        public String getString() {
            return decoder.getUTF8();
        }

        public boolean hasRemaining() {
            return decoder.hasRemaining();
        }

        public int getInt32() {
            return decoder.getInt32();
        }

        public int getReg() {
            return decoder.getUInt();
        }

        public int getBytecode() {
            return decoder.getUInt();
        }

        public ByteBuffer getBuffer() {
            return decoder.getBuffer();
        }

        public Object getArgument(int index) {
            return arguments[index];
        }
    }

    default Class<?>[] getClasses() {
        return new Class<?>[0];
    }

    Object decodeNode(DecoderState state, F factory);

    int getMethodIdFromSignature(String signature);

    int getChecksum();
}
