/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
